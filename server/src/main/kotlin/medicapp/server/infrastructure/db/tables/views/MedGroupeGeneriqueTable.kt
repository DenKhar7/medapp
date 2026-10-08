package medicapp.server.infrastructure.db.tables.views

import org.jetbrains.exposed.sql.Table

object MedGroupeGeneriqueTable : Table("med_groupe_generique_view") {
    val identifiant = varchar("identifiant", 50)
    val uri = varchar("uri", 255).nullable()
    val libelle = varchar("libelle", 255).nullable()
    override val primaryKey = PrimaryKey(identifiant, name = "pk_gg")
}