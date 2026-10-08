package medicapp.server.domain.models

import java.sql.Date

data class MedEvenementCip(
    val evenement: String,
    val cip13: String,
    val uri: String,
    val dateEffet: Date?,
    val dateNotification: Date?,
    val typeEvenement: String?,
    val description: String?
)