package medicapp.server.infrastructure.db.tables.views

import org.jetbrains.exposed.sql.Table

object MedSpecialiteVoieTable : Table("med_specialite_voie_view") {
    val cis = integer("cis")
        .references(MedSpecialiteTable.cis)
    val voieId = varchar("voie_id", 50)
        .references(MedVoieTable.identifiant)
        .index()
    override val primaryKey = PrimaryKey(cis, voieId, name = "pk_med_specialite_voie")
}