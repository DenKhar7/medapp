package medicapp.server.integration.config

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import medicapp.server.config.DbRegistry
import medicapp.server.config.MedDatabase
import medicapp.server.infrastructure.db.tables.views.*
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.extension.BeforeAllCallback
import org.junit.jupiter.api.extension.ExtensionContext
import org.testcontainers.containers.MariaDBContainer
import org.testcontainers.junit.jupiter.Container

/**
 * Extension JUnit 5 qui gère un conteneur MariaDB Testcontainer partagé pour les tests d'intégration.
 *
 * Cette extension :
 * - Démarre un conteneur MariaDB une seule fois par classe de test
 * - Configure DbRegistry avec la connexion au conteneur
 * - Crée le schéma de la base de données avec Exposed SchemaUtils
 * - Charge les données de test depuis les fichiers CSV
 *
 * Remarque : Si Docker n'est pas disponible, Testcontainers lèvera une exception avec un message explicite.
 */
class IntegrationTestExtension : BeforeAllCallback {

    override fun beforeAll(context: ExtensionContext) {
        // Initialiser une seule fois pour toutes les classes de test
        if (!isInitialized) {
            synchronized(lock) {
                if (!isInitialized) {
                    initializeContainer()
                    isInitialized = true
                }
            }
        }
    }

    companion object {
        private val lock = Any()
        private var isInitialized = false

        @Container
        val container: MariaDBContainer<*> = MariaDBContainer("mariadb:10.11")
            .withDatabaseName("med_db_views")
            .withUsername("test")
            .withPassword("test")
            .withReuse(true)

        lateinit var dataSource: HikariDataSource
            private set

        lateinit var database: Database
            private set

        private fun initializeContainer() {
            // Démarrer le conteneur s'il n'est pas en cours d'exécution
            if (!container.isRunning) {
                container.start()
            }

            // Créer la DataSource HikariCP
            dataSource = HikariDataSource(HikariConfig().apply {
                jdbcUrl = container.jdbcUrl
                username = container.username
                password = container.password
                driverClassName = "org.mariadb.jdbc.Driver"
                maximumPoolSize = 5
                isAutoCommit = false
                transactionIsolation = "TRANSACTION_REPEATABLE_READ"
            })

            // Connecter Exposed
            database = Database.connect(dataSource)

            // Enregistrer dans DbRegistry pour que dbViewQuery fonctionne
            DbRegistry.register(MedDatabase.VIEWS, database, dataSource)

            // Créer le schéma
            createSchema()

            // Charger les données de test
            TestDataLoader.loadAllTestData(dataSource)
        }

        private fun createSchema() {
            transaction(database) {
                // Créer les tables dans l'ordre des dépendances (pas de contraintes FK, donc l'ordre est moins critique)
                SchemaUtils.create(
                    MedOrganisationTable,
                    MedVoieTable,
                    DictionnaireSmsTable,
                    MedSubstanceTable,
                    MedSpecialiteTable,
                    MedSpecialiteVoieTable,
                    MedPresentationTable,
                    MedCompositionQualitativeTable,
                    MedCompositionQuantitativeTable,
                    MedEvenementCisTable,
                    MedEvenementCipTable,
                    MedElementSpecialiteTable,
                    MedElementPresentationTable
                )
            }
        }

        /**
         * Libère les ressources. Appelé automatiquement par Testcontainers.
         */
        fun cleanup() {
            if (::dataSource.isInitialized && !dataSource.isClosed) {
                dataSource.close()
            }
        }
    }
}
