package medicapp.server.infrastructure.db.requests

import medicapp.server.config.JdbcConnection
import medicapp.server.config.MedDatabase
import medicapp.server.config.StorageConfig
import medicapp.server.config.logger
import medicapp.server.infrastructure.db.constants.CsvPaths
import medicapp.server.infrastructure.db.enums.DataSourceEnum
import medicapp.server.infrastructure.db.enums.TableEnum
import medicapp.server.infrastructure.csv.importers.DictionnaireSmsImporter
import java.io.File
import kotlin.coroutines.cancellation.CancellationException


object ImportExecutionService {

    private val log = logger()

    fun importRuimData(
        database : MedDatabase,
        storageConfig: StorageConfig
    ) {
        val csvPaths = CsvPaths(storageConfig.dataDir)
        require(database in listOf(MedDatabase.TEMP, MedDatabase.ACTIVE))
        log.info("[RUIM] Démarrage de l'import des données dans la base $database")
        JdbcConnection(database).use { conn ->
            conn.autoCommit = false
            try {
                TableEnum.entries
                    .filter { it.source == DataSourceEnum.RUIM }
                    .sortedBy { it.order }
                    .forEach { table ->
                        table.importer?.let {
                            log.info("[RUIM] Import des données dans la table ${table.tableName}")
                            it(csvPaths, conn)
                            log.info("[RUIM] Import réussi des données dans la table ${table.tableName}")
                        }
                    }

                conn.commit()
                log.info("[RUIM] Import des données terminé avec succès pour la base $database")
            } catch (e: Exception) {
                log.error("[RUIM] Échec de l'import des données, annulation de la transaction : ${e.message}")
                conn.rollback()
                throw e
            }
        }
    }

    fun importSmsData(
        database : MedDatabase,
        storageConfig: StorageConfig
    ) {
        val csvPaths = CsvPaths(storageConfig.dataDir)
        require(database in listOf(MedDatabase.TEMP, MedDatabase.ACTIVE))
        log.info("[SMS] Démarrage de l'import des données dans la base $database")
        JdbcConnection(database).use { conn ->
            conn.autoCommit = false
            try {
                log.info("[SMS] Import des données dans la table dictionnaire_sms")
                DictionnaireSmsImporter.run(File(csvPaths.DICTIONNAIRE_SMS_PATH.toString()), conn)
                log.info("[SMS] Import réussi des données dans la table dictionnaire_sms")
                conn.commit()
                log.info("[SMS] Import des données terminé avec succès pour la base $database")
            } catch (e: CancellationException) {
                log.warn("[SMS] Import des données annulé, annulation de la transaction")
                conn.rollback()
                throw e
            } catch (e: Exception) {
                log.error("[SMS] Échec de l'import des données, annulation de la transaction : ${e.message}")
                conn.rollback()
                throw e
            }
        }
    }
}