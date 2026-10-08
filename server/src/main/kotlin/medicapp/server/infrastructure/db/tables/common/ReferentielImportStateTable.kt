package medicapp.server.infrastructure.db.tables.common

import medicapp.server.infrastructure.db.enums.DataSourceEnum
import medicapp.server.infrastructure.db.enums.ImportStatusEnum
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.kotlin.datetime.datetime

object ReferentielImportStateTable : Table("referentiel_import_state") {
    val id = long("id").autoIncrement()
    val data_source = enumerationByName(
        name = "source",
        length = 4,
        klass = DataSourceEnum::class
    )
    val version = varchar("version", 100)
    val fileHash = char("file_hash", 64)
    val importedAt = datetime("imported_at")

    val status = enumerationByName(
        name = "status",
        length = 10,
        klass = ImportStatusEnum::class
    )

    val message = text("message").nullable()

    override val primaryKey = PrimaryKey(id, name = "pk_referentiel_import_state")
}
