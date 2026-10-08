package medicapp.server.infrastructure.db.requests.dao

import medicapp.server.config.JdbcConnection
import medicapp.server.config.MedDatabase
import medicapp.server.infrastructure.db.enums.TableEnum

object TableMaintenanceDAO {

    fun dropTable(
        table: TableEnum,
        database: MedDatabase
    ) {
        val sql = "DROP TABLE IF EXISTS ${table.tableName}"

        JdbcConnection(database).use { conn ->
            conn.autoCommit = false
            try {
                conn.createStatement().use { it.execute(sql) }
                conn.commit()
            } catch (e: Exception) {
                conn.rollback()
                throw e
            }
        }
    }

    /**
     * Méthode permettant de supprimer toutes les tables dans la base de donnée med_db_archive
     * contenu dans la liste tables.
     * IMPORTANT : Cette méthode ne doit être utilisée que dans un contexte de maintenance
     * @param tables Liste de table à supprimer
     */
    fun dropTablesArchiveForce(
        tables: List<TableEnum>
    ) {
        tables.forEach { table ->
            JdbcConnection(MedDatabase.ARCHIVE).use { conn ->
                conn.autoCommit = false
                try {
                    conn.createStatement().use {
                        it.execute("SET FOREIGN_KEY_CHECKS = 0")
                    }

                    conn.createStatement().use {
                        it.execute("DROP TABLE IF EXISTS ${table.tableName}")
                    }

                    conn.createStatement().use {
                        it.execute("SET FOREIGN_KEY_CHECKS = 1")
                    }

                    conn.commit()
                } catch (e: Exception) {
                    conn.rollback()
                    throw e
                }
            }
        }
    }

    /**
     * Construit l'unique instruction RENAME TABLE réalisant la rotation de version pour [tables] :
     * active -> archive puis temp -> active.
     *
     * Les noms de tables viennent de [TableEnum] (constantes), jamais d'une saisie utilisateur.
     */
    internal fun buildSwapSql(tables: List<TableEnum>): String {
        require(tables.isNotEmpty()) { "Aucune table à basculer" }
        val renames = tables.joinToString(",\n") { table ->
            "med_db_active.${table.tableName} TO med_db_archive.${table.tableName},\n" +
                    "med_db_temp.${table.tableName} TO med_db_active.${table.tableName}"
        }
        return "RENAME TABLE\n$renames"
    }

    /**
     * Bascule ATOMIQUE de la version : toutes les tables de [tables] changent de base en une seule
     * instruction RENAME TABLE (tout ou rien). Les lectures de l'API (vues sur med_db_active) attendent
     * quelques instants le verrou de métadonnées au lieu de voir des tables manquantes, et en cas d'erreur
     * med_db_active reste intacte.
     *
     * Prérequis : les tables correspondantes n'existent plus dans med_db_archive (voir dropTablesArchiveForce).
     */
    fun swapTablesAtomically(tables: List<TableEnum>) {
        val sql = buildSwapSql(tables)

        JdbcConnection(MedDatabase.ACTIVE).use { conn ->
            conn.autoCommit = false
            try {
                conn.createStatement().use { it.execute(sql) }
                conn.commit()
            } catch (e: Exception) {
                conn.rollback()
                throw e
            }
        }
    }

    /** Nombre de lignes d'une table (utilisé pour vérifier la cohérence d'un import avant bascule). */
    fun countRows(table: TableEnum, database: MedDatabase): Long =
        JdbcConnection(database).use { conn ->
            conn.createStatement().use { st ->
                st.executeQuery("SELECT COUNT(*) FROM ${table.tableName}").use { rs ->
                    rs.next()
                    rs.getLong(1)
                }
            }
        }

    fun loadScriptAndCreateTable(tableToCreate: TableEnum) {
        val sql = this::class.java
            .getResource("/sql/${tableToCreate.sqlScriptCreate}")
            ?.readText()
            ?: throw IllegalStateException("Script SQL introuvable")

        JdbcConnection(MedDatabase.TEMP).use { conn ->
            conn.autoCommit = false
            try {
                conn.createStatement().use { it.execute(sql) }
                conn.commit()
            } catch (e: Exception) {
                conn.rollback()
                throw e
            }
        }
    }
}