package medicapp.server.integration.db

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import medicapp.server.config.DbRegistry
import medicapp.server.config.MedDatabase
import medicapp.server.infrastructure.db.enums.DataSourceEnum
import medicapp.server.infrastructure.db.enums.TableEnum
import medicapp.server.infrastructure.db.requests.ReferentielTableManager
import medicapp.server.infrastructure.db.requests.dao.TableMaintenanceDAO
import org.jetbrains.exposed.sql.Database
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.MethodOrderer
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestMethodOrder
import org.junit.jupiter.api.assertThrows
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.wait.strategy.Wait
import org.testcontainers.images.builder.Transferable
import org.testcontainers.utility.MountableFile
import java.nio.file.Files
import java.nio.file.Paths
import java.sql.Connection
import java.sql.DriverManager
import java.sql.SQLException
import java.time.Duration
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Teste, sur une vraie base MariaDB initialisée avec les scripts RÉELS de infra/sql/prod :
 *  - les droits des deux comptes applicatifs (API en lecture seule, import limité) ;
 *  - la bascule atomique des tables (clés étrangères conservées, tables vides recréées dans temp) ;
 *  - qu'une bascule en échec laisse la production intacte ;
 *  - le garde-fou refusant un import vide ou tronqué.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
class SchemaPrivilegesAndSwapIT {

    private class MariaDb(image: String) : GenericContainer<MariaDb>(image)

    companion object {
        private const val ROOT_PW = "rootpw"
        private const val IMPORT_USER = "import_user"
        private const val IMPORT_PW = "importpw"
        private const val API_USER = "api_user"
        private const val API_PW = "apipw"

        private lateinit var container: MariaDb
        private val pools = mutableListOf<HikariDataSource>()

        @BeforeAll
        @JvmStatic
        fun startDatabase() {
            val initDir = Paths.get("..", "infra", "sql", "prod")
            val initFiles = Files.list(initDir).use { stream -> stream.sorted().toList() }
            check(initFiles.isNotEmpty()) { "Scripts d'init introuvables dans $initDir" }

            container = MariaDb("mariadb:11")
                .withEnv("MARIADB_ROOT_PASSWORD", ROOT_PW)
                .withEnv("MARIADB_USER", IMPORT_USER)
                .withEnv("MARIADB_PASSWORD", IMPORT_PW)
                .withEnv("API_DB_USER", API_USER)
                .withEnv("API_DB_PASSWORD", API_PW)
                .withExposedPorts(3306)
                .waitingFor(Wait.forLogMessage(".*ready for connections.*\\n", 2))
                .withStartupTimeout(Duration.ofMinutes(3))
            initFiles.forEach { file ->
                val mode = if (file.fileName.toString().endsWith(".sh")) 493 else 420 // 0755 / 0644
                container.withCopyFileToContainer(
                    MountableFile.forHostPath(file, mode),
                    "/docker-entrypoint-initdb.d/${file.fileName}"
                )
            }
            container.start()

            // Mêmes caractéristiques que la production : un compte d'import, une connexion par base.
            mapOf(
                MedDatabase.ACTIVE to "med_db_active",
                MedDatabase.TEMP to "med_db_temp",
                MedDatabase.ARCHIVE to "med_db_archive"
            ).forEach { (type, dbName) ->
                val ds = HikariDataSource(HikariConfig().apply {
                    jdbcUrl = jdbcUrl(dbName)
                    username = IMPORT_USER
                    password = IMPORT_PW
                    driverClassName = "org.mariadb.jdbc.Driver"
                    maximumPoolSize = 1
                    isAutoCommit = false
                })
                pools += ds
                DbRegistry.register(type, Database.connect(ds), ds)
            }
        }

        @AfterAll
        @JvmStatic
        fun stopDatabase() {
            pools.forEach { it.close() }
            container.stop()
        }

        private fun jdbcUrl(db: String = "") =
            "jdbc:mariadb://${container.host}:${container.getMappedPort(3306)}/$db"

        private fun connect(user: String, password: String): Connection =
            DriverManager.getConnection(jdbcUrl(), user, password).apply { autoCommit = true }

        private fun root() = connect("root", ROOT_PW)
        private fun api() = connect(API_USER, API_PW)
        private fun importer() = connect(IMPORT_USER, IMPORT_PW)
    }

    private fun Connection.exec(sql: String) = createStatement().use { it.execute(sql) }

    private fun Connection.longs(sql: String): List<Long> =
        createStatement().use { st -> st.executeQuery(sql).use { rs -> buildList { while (rs.next()) add(rs.getLong(1)) } } }

    private fun Connection.strings(sql: String): List<String> =
        createStatement().use { st -> st.executeQuery(sql).use { rs -> buildList { while (rs.next()) add(rs.getString(1)) } } }

    private fun Connection.count(sql: String) = longs(sql).single()

    private val ruimTables = TableEnum.entries.filter { it.source == DataSourceEnum.RUIM }

    // ─────────────────────────────────────────────────────────────
    // Comptes applicatifs
    // ─────────────────────────────────────────────────────────────

    @Test
    @Order(1)
    fun `le compte API lit les vues mais rien d autre`() {
        api().use { c ->
            assertEquals(0, c.count("SELECT COUNT(*) FROM med_db_views.med_specialite_view"))

            assertThrows<SQLException>("lecture d'une table de base interdite") {
                c.exec("SELECT * FROM med_db_active.med_specialite")
            }
            assertThrows<SQLException>("écriture interdite") {
                c.exec("DELETE FROM med_db_views.med_specialite_view")
            }
            assertThrows<SQLException>("création interdite") {
                c.exec("CREATE TABLE med_db_views.intrus (a INT)")
            }
        }
    }

    @Test
    @Order(2)
    fun `le compte d import ecrit dans les tables mais ne touche ni aux vues ni aux droits`() {
        importer().use { c ->
            c.exec("INSERT INTO med_db_active.med_voie (identifiant, uri, libelle) VALUES ('TMP', 'u', 'tmp')")
            c.exec("DELETE FROM med_db_active.med_voie WHERE identifiant = 'TMP'")

            assertThrows<SQLException>("les vues ne lui sont pas accessibles") {
                c.exec("SELECT * FROM med_db_views.med_specialite_view")
            }
            assertThrows<SQLException>("il ne peut pas s'octroyer de droits") {
                c.exec("GRANT ALL PRIVILEGES ON *.* TO '$IMPORT_USER'@'%'")
            }
            assertThrows<SQLException>("il n'a aucun droit sur les autres bases") {
                c.exec("SELECT * FROM mysql.user")
            }

            val grants = c.strings("SHOW GRANTS FOR CURRENT_USER()").joinToString("\n")
            assertTrue("ALL PRIVILEGES" !in grants, "Aucun ALL PRIVILEGES attendu :\n$grants")
        }
    }

    // ─────────────────────────────────────────────────────────────
    // Garde-fou + bascule atomique
    // ─────────────────────────────────────────────────────────────

    @Test
    @Order(3)
    fun `la bascule remplace la production en une fois, conserve les cles etrangeres et recree temp`() {
        root().use { c ->
            // Production actuelle (4 spécialités) et nouvelle version dans temp
            c.exec("INSERT INTO med_db_active.med_voie (identifiant, uri, libelle) VALUES ('OLD', 'u', 'ancienne voie')")
            (1..4).forEach {
                c.exec("INSERT INTO med_db_active.med_specialite (cis, uri, libelle) VALUES ($it, 'u', 'OLD $it')")
            }
            c.exec("INSERT INTO med_db_active.med_specialite_voie (cis, voie_id) VALUES (1, 'OLD')")

            c.exec("INSERT INTO med_db_temp.med_voie (identifiant, uri, libelle) VALUES ('NEW', 'u', 'nouvelle voie')")
            c.exec("INSERT INTO med_db_temp.med_specialite (cis, uri, libelle) VALUES (11, 'u', 'NEW 11')")
            c.exec("INSERT INTO med_db_temp.med_specialite_voie (cis, voie_id) VALUES (11, 'NEW')")

            // Garde-fou : 1 ligne contre 4 en production (< 50 %) => refusé
            assertThrows<IllegalStateException>("import tronqué") {
                ReferentielTableManager.assertImportIsSane(DataSourceEnum.RUIM)
            }

            c.exec("INSERT INTO med_db_temp.med_specialite (cis, uri, libelle) VALUES (12, 'u', 'NEW 12')")
            c.exec("INSERT INTO med_db_temp.med_specialite (cis, uri, libelle) VALUES (13, 'u', 'NEW 13')")
            ReferentielTableManager.assertImportIsSane(DataSourceEnum.RUIM) // 3 >= 50 % de 4 : accepté

            ReferentielTableManager.switchTables(DataSourceEnum.RUIM)

            // Production = nouvelle version, archive = ancienne version
            assertEquals(listOf(11L, 12L, 13L), c.longs("SELECT cis FROM med_db_active.med_specialite ORDER BY cis"))
            assertEquals(listOf(1L, 2L, 3L, 4L), c.longs("SELECT cis FROM med_db_archive.med_specialite ORDER BY cis"))
            assertEquals(listOf("NEW"), c.strings("SELECT identifiant FROM med_db_active.med_voie"))
            assertEquals(listOf("OLD"), c.strings("SELECT identifiant FROM med_db_archive.med_voie"))

            // Toutes les tables RUIM existent dans les trois bases ; temp est de nouveau vide et prête
            ruimTables.forEach { t ->
                listOf("med_db_active", "med_db_archive", "med_db_temp").forEach { db ->
                    assertEquals(
                        1,
                        c.count("SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='$db' AND table_name='${t.tableName}'"),
                        "${t.tableName} absente de $db"
                    )
                }
            }
            assertEquals(0, c.count("SELECT COUNT(*) FROM med_db_temp.med_specialite"))

            // Les clés étrangères de la production nouvellement basculée sont toujours appliquées
            assertThrows<SQLException>("cis inexistant") {
                c.exec("INSERT INTO med_db_active.med_specialite_voie (cis, voie_id) VALUES (999, 'NEW')")
            }
            c.exec("INSERT INTO med_db_active.med_specialite_voie (cis, voie_id) VALUES (12, 'NEW')")
        }

        // L'API (compte lecture seule, via les vues) voit immédiatement la nouvelle version
        api().use { c ->
            assertEquals(
                listOf("NEW 11", "NEW 12", "NEW 13"),
                c.strings("SELECT libelle FROM med_db_views.med_specialite_view ORDER BY cis")
            )
        }
    }

    @Test
    @Order(4)
    fun `une bascule en echec laisse la production intacte`() {
        root().use { c ->
            val before = c.longs("SELECT cis FROM med_db_active.med_specialite ORDER BY cis")

            // On prépare une bascule impossible : une table source manque dans temp
            ReferentielTableManager.dropTablesArchive(DataSourceEnum.RUIM)
            c.exec("SET FOREIGN_KEY_CHECKS = 0")
            c.exec("DROP TABLE med_db_temp.med_evenement_cis")
            c.exec("SET FOREIGN_KEY_CHECKS = 1")

            assertThrows<SQLException> { TableMaintenanceDAO.swapTablesAtomically(ruimTables) }

            // Rien n'a bougé : mêmes données, toutes les tables toujours présentes dans active
            assertEquals(before, c.longs("SELECT cis FROM med_db_active.med_specialite ORDER BY cis"))
            ruimTables.forEach { t ->
                assertEquals(
                    1,
                    c.count("SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='med_db_active' AND table_name='${t.tableName}'"),
                    "${t.tableName} a disparu de la production après un échec"
                )
            }
        }
    }

    @Test
    @Order(5)
    fun `le garde fou refuse un import vide`() {
        // temp.med_specialite est vide depuis la bascule : rien à mettre en production
        assertThrows<IllegalStateException> {
            ReferentielTableManager.assertImportIsSane(DataSourceEnum.RUIM)
        }
    }
}
