package medicapp.server.domain.models

data class MedVoie(
    val identifiant: String,
    val uri: String,
    val libelle: String,
    val codeRms: String?,
    val codeEdqm: String?
)