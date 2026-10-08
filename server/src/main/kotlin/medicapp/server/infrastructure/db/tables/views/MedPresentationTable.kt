package medicapp.server.infrastructure.db.tables.views

import org.jetbrains.exposed.sql.Table

object MedPresentationTable : Table("med_presentation_view") {
    val cip13 = varchar("cip13", 13)
    val cip7 = varchar("cip7", 7).nullable()
    val uri = varchar("uri", 255)
    val cis = integer("cis").references(MedSpecialiteTable.cis).index()
    val libelle = varchar("libelle", 510)
    val altLabel = varchar("alt_label", 255).nullable()
    val quantiteConditionnement = decimal("quantite_conditionnement", 10, 2).nullable()
    val uniteConditionnement = varchar("unite_conditionnement", 50).nullable()
    val nbUniteDisp = integer("nb_unite_disp").nullable()
    val typeDispositif = varchar("type_dispositif", 200).nullable()
    override val primaryKey = PrimaryKey(cip13, name = "cip13")
}