package medicapp.server.domain.businessclass.medicament

import kotlinx.serialization.Serializable

@Serializable
data class MedicamentSpeResume(
    val cis: Int,
    val nom: String,
    val nomOrganisation: String?,
    val codeAtc: String?,
    val libelleAtc: String?,
    val voies: List<String>,
)