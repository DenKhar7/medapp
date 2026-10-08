package medicapp.server.domain.businessclass.medicament

import kotlinx.serialization.Serializable

@Serializable
data class MedicamentPresDetail(
    val cip13: String, // identifiant
    val label: String, // libellé ANSM de la présentation
    val nomSpecialite: String,
    val cis: Int,
    val nomOrganisation: String?,
    val codeAtc: String?,
    val libelleAtc: String?,
    val voie: List<String>,

    // Informations physiques de la boîte
    val dosesParBoite: Int?,
    val quantiteConditionnement: Double?,
    val uniteConditionnement: String?,
    val typeDispositif: String?,
)
