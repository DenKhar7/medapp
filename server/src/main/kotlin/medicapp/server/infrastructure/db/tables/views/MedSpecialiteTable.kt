package medicapp.server.infrastructure.db.tables.views

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.kotlin.datetime.date

object MedSpecialiteTable : Table("med_specialite_view") {
    val cis = integer("cis")
    val uri = varchar("uri", length = 255)
    val libelle = varchar("libelle", 255)
    val actif = bool("actif").default(true)
    val dateDebut = date("date_debut").nullable()
    val dateFin = date("date_fin").nullable()
    val codeAtc = varchar("code_atc", 15).nullable()
    val libelleAtc = varchar("libelle_atc", 255).nullable()
    val typeProcedure = varchar("type_procedure", 255).nullable()
    val statutCourant = varchar("statut_courant", 100).nullable()
    val titulaireId = varchar("titulaire_id", 50).nullable()
    val niveauVirtualisation = varchar("niveau_virtualisation", 50).nullable()
    val prescriptibiliteDc = varchar("prescriptibilite_dc", 100).nullable()
    val formeManufacturee = varchar("forme_manufacturee", 255).nullable()
    override val primaryKey = PrimaryKey(cis, name = "cis")
}