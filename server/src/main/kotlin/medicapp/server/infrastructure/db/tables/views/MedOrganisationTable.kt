package medicapp.server.infrastructure.db.tables.views

import org.jetbrains.exposed.sql.Table

object MedOrganisationTable : Table("med_organisation_view") {
    val identifiant = varchar("identifiant", 50)
    val uri = varchar("uri", 255).nullable()
    val libelle = varchar("libelle", 255).nullable()
    val pays = varchar("pays", 100).nullable()
    override val primaryKey = PrimaryKey(identifiant, name = "pk_org")
}