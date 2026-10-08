package medicapp.server.domain.models

data class MedElementSpecialite(
    val uri : String,
    val cis : Int,
    val identifiantElement : Int,
    val libelle : String,
    val formeManufactureeLitterale : String
)