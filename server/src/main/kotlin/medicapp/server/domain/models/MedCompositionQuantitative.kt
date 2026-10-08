package medicapp.server.domain.models

data class MedCompositionQuantitative(
    val cis : Int,
    val identifiantElement : String,
    val codeSubstance : String,
    val expressionQuantite : String?,
    val refereneceDosage : String?,
    val substanceActive : Int?,
    val fractionTherapeutique : Int?
)
