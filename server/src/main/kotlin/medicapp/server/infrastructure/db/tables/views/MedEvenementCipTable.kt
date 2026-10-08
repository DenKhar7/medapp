package medicapp.server.infrastructure.db.tables.views

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.kotlin.datetime.date

object MedEvenementCipTable : Table("med_evenement_cip_view") {
    val evenement = varchar("evenement", 255)
    val uri = varchar("uri", 255)
    val cip13 = varchar("cip13", 13).references(MedPresentationTable.cip13).index()
    val dateEffet = date("date_effet").nullable()
    val dateNotification = date("date_notification").nullable()
    val typeEvenement = varchar("type_evenement", 100).nullable()
    override val primaryKey = PrimaryKey(evenement, name = "pk_med_evenement_cip")
}