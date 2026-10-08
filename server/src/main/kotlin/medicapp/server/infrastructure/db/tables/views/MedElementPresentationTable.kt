package medicapp.server.infrastructure.db.tables.views

import org.jetbrains.exposed.sql.Table

object MedElementPresentationTable : Table("med_element_presentation_view") {
    val uri = varchar("uri", 255)
    val cip13 = varchar("cip13", 13)
        .references(MedPresentationTable.cip13)
    val identifiantElement = integer("identifiant_element")
    val libelle = varchar("libelle", 510)
    val typeContenantLitteral = text("type_contenant_litteral")
    val typeContenant = varchar("type_contenant", 255)
    val formeAdministrable = text("forme_administrable")
    val unitePresentation = text("unite_presentation")
    val typeDose = varchar("type_dose", 255)
    override val primaryKey = PrimaryKey(cip13, identifiantElement, name = "pk_med_element_presentation")

}