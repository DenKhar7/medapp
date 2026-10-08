package medicapp.server.domain.models

import java.sql.Date

data class MedEvenementCis(
    val evenement: String,
    val cis: Int,
    val uri: String,
    val dateEffet: Date?,
    val dateNotification: Date?,
    val typeEvenement: String?,
    val description: String?
)