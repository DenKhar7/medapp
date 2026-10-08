package medicapp.server.infrastructure.db.tables.views

import org.jetbrains.exposed.sql.Table

object MedElementSpecialiteTable : Table("med_element_specialite_view") {
    val uri = varchar("uri", 255)
    val cis = integer("cis")
        .references(MedSpecialiteTable.cis)
    val identifiantElement = integer("identifiant_element")
    val libelle = varchar("libelle", 510)
    val formeManufactureeLitterale = text("forme_manufacturee_litterale")
    override val primaryKey = PrimaryKey(cis, identifiantElement, name = "pk_med_element_specialite")
}