package medicapp.server.domain.models

import java.sql.Date

data class MedSpecialite(
    val cis: Int,
    val uri: String,
    val libelle: String,
    val actif: Boolean?,
    val dateDebut: Date?,
    val dateFin: Date?,
    val codeATC: String?,
    val libelleATC: String?,
    val typeProcedure: String?,
    val statutCourant: String?,
    val titulaireId: String?,
    val niveauVirtualisation: String?,
    val prescriptibiliteDc: String?,
    val formeManufacturee: String?
)
