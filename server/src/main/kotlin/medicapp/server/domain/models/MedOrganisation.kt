package medicapp.server.domain.models

data class MedOrganisation(
    val identifiant : String,
    val uri : String,
    val libelle : String,
    val pays : String?
)
