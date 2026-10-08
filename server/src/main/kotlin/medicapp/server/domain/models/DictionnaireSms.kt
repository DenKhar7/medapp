package medicapp.server.domain.models

import java.util.Date

data class DictionnaireSms(
    val codeSms: String,
    val nomSubstance: String,
    val isTermePref: Boolean?,
    val sourceNom: String?,
    val statusSubstance: String?,
    val typeSubstance: String?,
    val formuleMoleculaire: String?,
    val poidsMoleculaire: Float?,
    val inchikey: String?,
    val lastUpdateDate: Date?,
)
