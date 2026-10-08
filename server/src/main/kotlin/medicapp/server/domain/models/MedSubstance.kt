package medicapp.server.domain.models

data class MedSubstance(
    val codeSubstance: String,
    val uri : String,
    val libelleFr : String,
    val codeSms : String?,
    val synonymesFr : String?
)
