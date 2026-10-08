package medicapp.server.domain.businessclass.medicament

import kotlinx.serialization.Serializable

@Serializable
data class MedicamentSubstanceDetail(
    val nomSubstance : String,
    val typeSubstance : String?,
    val formuleMoleculaire : String?,
    val poidMoleculaire : Float?
)