package medicapp.server.domain.businessclass.medicament

import kotlinx.serialization.Serializable

@Serializable
data class MedicamentSubstanceResume(
    val codeSubstance: String,
    val libelle: String,
    val relationSubstance: String,
    val expressionQuantite : String?,
    val substanceActive : Int?,
    val fractionTherapeutique : Int?
)
