package medicapp.server.domain.models

data class MedCompositionQualitative(
    val cis : Int,
    val identifiantElement : String,
    val relationSubstance : String,
    val codeSubstance : String,
)
