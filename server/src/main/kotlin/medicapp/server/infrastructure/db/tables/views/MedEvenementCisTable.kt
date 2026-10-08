package medicapp.server.infrastructure.db.tables.views

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.kotlin.datetime.date

object MedEvenementCisTable : Table("med_evenement_cis_view") {
    val evenement = varchar("evenement", 255)
    val uri = varchar("uri", 255)
    val cis = integer("cis").references(MedSpecialiteTable.cis).index()
    val dateEffet = date("date_effet").nullable()
    val dateNotification = date("date_notification").nullable()
    val typeEvenement = varchar("type_evenement", 100).nullable()
    override val primaryKey = PrimaryKey(evenement, name = "pk_med_evenement_cis")
}