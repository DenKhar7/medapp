package iut.butinfo3.app_mobile.utils

import iut.butinfo3.app_mobile.model.entity.RecurrenceUnit
import java.util.Calendar
import java.util.Date
import java.util.concurrent.TimeUnit

/**
 * Utilitaire permettant de gérer les calculs de récurrence pour les rappels de traitement.
 * Cette classe contient la logique métier pour déterminer si une instance de rappel
 * doit être déclenchée à la date actuelle.
 */
object RecurrenceUtils {

    /**
     * Vérifie si un rappel est dû aujourd'hui en fonction des paramètres de récurrence.
     * @param treatmentStartDate La date à laquelle le traitement a commencé.
     * @param interval L'intervalle de répétition (ex: tous les 2 jours, toutes les 3 semaines). Par défaut 1.
     * @param unit L'unité de récurrence (quotidienne ou un jour spécifique de la semaine). Par défaut DAY.
     * @return [Boolean] Vrai si le rappel doit être déclenché aujourd'hui, faux sinon.
     */
    /**
     * Vérifie si un rappel est dû à une date arbitraire en fonction des paramètres de récurrence.
     * @param targetDate La date à vérifier.
     * @param treatmentStartDate La date à laquelle le traitement a commencé.
     * @param interval L'intervalle de répétition (ex: tous les 2 jours, toutes les 3 semaines). Par défaut 1.
     * @param unit L'unité de récurrence (quotidienne ou un jour spécifique de la semaine). Par défaut DAY.
     * @return [Boolean] Vrai si le rappel doit être déclenché à la date cible, faux sinon.
     */
    fun isReminderDueOnDate(
        targetDate: Date,
        treatmentStartDate: Date,
        interval: Int = 1,
        unit: RecurrenceUnit = RecurrenceUnit.DAY
    ): Boolean {
        val target = getStartOfDay(targetDate)
        val start = getStartOfDay(treatmentStartDate)

        if (target.before(start)) return false

        val cal = Calendar.getInstance().apply { time = target }
        val currentDayOfWeek = cal[Calendar.DAY_OF_WEEK]

        if (unit == RecurrenceUnit.DAY) {
            val diffDays = getDiffDays(start, target)
            return (diffDays % interval) == 0L
        }

        val targetDayOfWeek = getCalendarDayConstant(unit)

        if (targetDayOfWeek != -1) {
            if (currentDayOfWeek != targetDayOfWeek) {
                return false
            }

            val diffDays = getDiffDays(start, target)
            val diffWeeks = diffDays / 7

            return (diffWeeks % interval) == 0L
        }

        return true
    }

    fun isReminderDueToday(
        treatmentStartDate: Date,
        interval: Int = 1,
        unit: RecurrenceUnit = RecurrenceUnit.DAY
    ): Boolean {
        return isReminderDueOnDate(Date(), treatmentStartDate, interval, unit)
    }

    private fun getDiffDays(start: Date, end: Date): Long {
        val diffInMillis = end.time - start.time
        return TimeUnit.MILLISECONDS.toDays(diffInMillis)
    }

    private fun getCalendarDayConstant(unit: RecurrenceUnit): Int {
        return when (unit) {
            RecurrenceUnit.SUNDAY -> Calendar.SUNDAY
            RecurrenceUnit.MONDAY -> Calendar.MONDAY
            RecurrenceUnit.TUESDAY -> Calendar.TUESDAY
            RecurrenceUnit.WEDNESDAY -> Calendar.WEDNESDAY
            RecurrenceUnit.THURSDAY -> Calendar.THURSDAY
            RecurrenceUnit.FRIDAY -> Calendar.FRIDAY
            RecurrenceUnit.SATURDAY -> Calendar.SATURDAY
            RecurrenceUnit.DAY -> -1
        }
    }

    private fun getStartOfDay(date: Date): Date {
        val cal = Calendar.getInstance().apply {
            time = date
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return cal.time
    }
}