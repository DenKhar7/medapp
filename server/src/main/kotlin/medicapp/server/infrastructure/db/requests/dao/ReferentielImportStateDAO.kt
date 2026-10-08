package medicapp.server.infrastructure.db.requests.dao

import kotlinx.datetime.LocalDateTime
import medicapp.server.config.MedDatabase
import medicapp.server.config.dbQuery
import medicapp.server.infrastructure.db.enums.DataSourceEnum
import medicapp.server.infrastructure.db.enums.ImportStatusEnum
import medicapp.server.infrastructure.db.tables.common.ReferentielImportStateTable
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.update

object ReferentielImportStateDAO {

    suspend fun getState(
        source: DataSourceEnum,
        database: MedDatabase
    ): ResultRow =
        dbQuery(database) {
            ReferentielImportStateTable
                .selectAll()
                .where { ReferentielImportStateTable.data_source eq source }
                .singleOrNull()
                ?: error("No active referential state for $source")
        }

    suspend fun insertState(
        database: MedDatabase,
        source: DataSourceEnum,
        version: String,
        fileHash: String,
        importedAt: LocalDateTime,
        status: ImportStatusEnum,
        message: String? = null,
    ) {
        require(database != MedDatabase.VIEWS)
        dbQuery(database) {
            ReferentielImportStateTable.insert {
                it[ReferentielImportStateTable.version] = version
                it[ReferentielImportStateTable.fileHash] = fileHash
                it[ReferentielImportStateTable.importedAt] = importedAt
                it[ReferentielImportStateTable.status] = status
                it[ReferentielImportStateTable.message] = message
                it[ReferentielImportStateTable.data_source] = source
            }
        }
    }

    /**
     * Enregistre l'échec de la dernière mise à jour d'un référentiel, sans toucher à la version ni au hash
     * actuellement en production (les données restent celles de la dernière mise à jour réussie).
     */
    suspend fun markFailed(
        source: DataSourceEnum,
        reason: String,
        database: MedDatabase = MedDatabase.ACTIVE
    ) {
        require(database != MedDatabase.VIEWS)
        dbQuery(database) {
            ReferentielImportStateTable.update(
                { ReferentielImportStateTable.data_source eq source }
            ) {
                it[status] = ImportStatusEnum.FAILED
                it[message] = reason.take(MAX_MESSAGE_LENGTH)
            }
        }
    }

    private const val MAX_MESSAGE_LENGTH = 2000

    suspend fun updateState(
        source: DataSourceEnum,
        newVersion: String,
        newFileHash: String,
        importedAt: LocalDateTime,
        newStatus: ImportStatusEnum,
        database : MedDatabase,
        message: String? = null,
    ) {
        require(database != MedDatabase.VIEWS)
        dbQuery(database) {
            ReferentielImportStateTable.update(
                { ReferentielImportStateTable.data_source eq source }
            ) {
                it[version] = newVersion
                it[fileHash] = newFileHash
                it[ReferentielImportStateTable.importedAt] = importedAt
                it[status] = newStatus
                it[ReferentielImportStateTable.message] = message
            }
        }
    }
}