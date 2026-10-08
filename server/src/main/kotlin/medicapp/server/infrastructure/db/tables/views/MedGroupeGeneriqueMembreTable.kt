package medicapp.server.infrastructure.db.tables.views

import org.jetbrains.exposed.sql.Table

object MedGroupeGeneriqueMembreTable : Table("med_groupe_generique_membre_view") {
    val identifiantGroupe = varchar("identifiant_groupe", 50)
        .references(MedGroupeGeneriqueTable.identifiant)
    val cis = integer("cis")
        .references(MedSpecialiteTable.cis)
    val statut = varchar("statut", 100).nullable()
    override val primaryKey = PrimaryKey(identifiantGroupe, cis, name = "pk_gm")
}