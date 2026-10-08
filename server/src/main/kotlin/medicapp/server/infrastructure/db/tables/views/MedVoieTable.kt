package medicapp.server.infrastructure.db.tables.views

import org.jetbrains.exposed.sql.Table

object MedVoieTable : Table("med_voie_view") {
    val identifiant = varchar("identifiant", 50)
    val uri = varchar("uri", 255)
    val libelle = varchar("libelle", 255)
    val codeRms = varchar("code_rms", 50).nullable()
    val codeEdqm = varchar("code_edqm", 50).nullable()
    override val primaryKey = PrimaryKey(identifiant, name = "identifiant_pk")
}
