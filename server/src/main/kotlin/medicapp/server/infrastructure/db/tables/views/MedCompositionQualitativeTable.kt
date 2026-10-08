package medicapp.server.infrastructure.db.tables.views

import org.jetbrains.exposed.sql.Table

object MedCompositionQualitativeTable : Table("med_composition_qualitative_view") {
    val cis = integer("cis")
        .references(MedSpecialiteTable.cis)
    val identifiantElement = varchar("identifiant_element", 50)
    val relationSubstance = varchar("relation_substance", 50)
    val codeSubstance = varchar("code_substance", 50)
    override val primaryKey = PrimaryKey(cis, identifiantElement, codeSubstance, name = "pk_cis_id_elemnt_codesub")
}