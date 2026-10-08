package medicapp.server.domain.businessclass.medicament

import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable

@Serializable
data class MedicamentPresStatus(
    val cip13 : String,
    val dateEffet: LocalDate?,
    val typeEvenement: String?,
)