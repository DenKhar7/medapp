package medicapp.server.domain.models

data class MedPresentation(
    val cip13: String,
    val cip7: String?,
    val cis: Int,
    val uri: String,
    val libelle: String,
    val altLabel: String?,
    val quantiteConditionnement : Float?,
    val uniteConditionnement : String?,
    val nbUniteDisp: Int?,
    val typeDispositif: String?,
)
