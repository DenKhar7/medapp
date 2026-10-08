package medicapp.server.domain.businessclass.medicament

import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable

@Serializable
data class MedicamentSpeStatus(
    val cis : Int,
    val dateEffet: LocalDate?,
    val typeEvenement: String?,
)