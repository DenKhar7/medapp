package medicapp.server.infrastructure.db.tables.views

import org.jetbrains.exposed.sql.Table

object MedSubstanceTable : Table("med_substance_view") {
    val codeSubstance = varchar("code_substance", 50)
    val uri = varchar("uri", 255)
    val libelleFr = varchar("libelle_fr", 255)
    val codeSms = varchar("code_sms", 50)
        .references(DictionnaireSmsTable.codeSms)
        .nullable()
        .index()
    val synonymesFr = text("synonymes_fr").nullable()
    override val primaryKey = PrimaryKey(codeSubstance, name = "pk_sub")
}