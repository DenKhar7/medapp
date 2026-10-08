package iut.butinfo3.app_mobile.model.entity

/**
 * Enum représentant les unités de récurrence pour les rappels de médicaments.
 *
 * - [DAY]: Récurrence quotidienne (tous les N jours)
 * - Les jours de la semaine (MONDAY, TUESDAY, etc.): Récurrence hebdomadaire sur ce jour
 */
enum class RecurrenceUnit {
    DAY,
    MONDAY,
    TUESDAY,
    WEDNESDAY,
    THURSDAY,
    FRIDAY,
    SATURDAY,
    SUNDAY;

    companion object {
        /**
         * Convertit une chaîne en RecurrenceUnit de manière sécurisée.
         * Retourne [DAY] par défaut si la valeur est invalide.
         */
        fun fromString(value: String?): RecurrenceUnit {
            return try {
                value?.let { valueOf(it.uppercase()) } ?: DAY
            } catch (e: IllegalArgumentException) {
                DAY
            }
        }
    }
}
