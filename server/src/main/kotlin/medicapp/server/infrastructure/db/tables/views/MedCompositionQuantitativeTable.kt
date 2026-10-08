package medicapp.server.infrastructure.db.tables.views

import org.jetbrains.exposed.sql.Table

object MedCompositionQuantitativeTable : Table("med_composition_quantitative_view") {
    val cis = integer("cis")
        .references(MedSpecialiteTable.cis)
    val identifiantElement = varchar("identifiant_element", 50)
    val codeSubstance = varchar("code_substance", 50)
    val expressionQuantite = varchar("expression_quantite", 255).nullable()
    val referenceDosage = varchar("reference_dosage", 255).nullable()
    val substanceActive = integer("substance_active").nullable()
    val fractionTherapeutique = integer("fraction_therapeutique").nullable()
    override val primaryKey = PrimaryKey(cis, identifiantElement, codeSubstance, name = "pk_cis_id_elemnt_codesub")
}