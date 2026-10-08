package medicapp.server.infrastructure.db.tables.views

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.kotlin.datetime.date

object DictionnaireSmsTable : Table("dictionnaire_sms_view"){
    val codeSms = varchar("code_sms", 50)
    val nomSubstance = varchar("nom_substance", 510).index()
    val isTermePref = bool("is_terme_pref").nullable()
    val sourceNom = varchar("source_nom", 255).nullable()
    val statusSubstance = varchar("status_substance", 255).nullable()
    val typeSubstance = varchar("type_substance", 255).nullable()
    val formuleMoleculaire = varchar("formule_moleculaire", 50).nullable()
    val poidMoleculaire = float("poid_moleculaire").nullable()
    val inchikey = float("inchikey").nullable()
    val lastUpdateDate = date("last_update_date")
    override val primaryKey = PrimaryKey(codeSms, nomSubstance, name = "pk_cis_nomSubstance")
}