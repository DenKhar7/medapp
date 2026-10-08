package medicapp.server.domain.businessclass.medicament

import kotlinx.serialization.Serializable

@Serializable
data class MedicamentLibelleCompletion(
    val cis : Int,
    val libelle : String
)
