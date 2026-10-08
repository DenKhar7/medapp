package iut.butinfo3.app_mobile.model

/**
 * Utilisé uniquement dans le treatmentDao pour récupérer les traitemnts journalier
 * permet de renvoyer de données au bon format, sans s'embéter à renvoyer des éléménets complexes
 */
data class DailyReminder(
    val reminderId: Int,
    val timeOfDay: String,
    val doseQuantity: String,
    val drugName: String,
    val drugCip: String,
    val treatmentId: Int
)