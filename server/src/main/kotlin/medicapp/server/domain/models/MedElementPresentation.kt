package medicapp.server.domain.models

data class MedElementPresentation(
    val uri : String,
    val cip13 : String,
    val identifiantElement : Int,
    val libelle : String,
    val typeContenantLitteral : String,
    val typeContenant : String?,
    val formeAdministrable : String?,
    val unitePresentation : String?,
    val typeDose : String?,
)
