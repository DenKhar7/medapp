package iut.butinfo3.app_mobile.calendar

import iut.butinfo3.app_mobile.view.adapter.CalendarDay
import iut.butinfo3.app_mobile.view.adapter.MedicationStatus
import java.util.Calendar
import java.util.Date

class CalendarService {

    /**
     * Génère la liste des 42 jours (6 semaines) pour la grille du calendrier.
     *
     * @param currentCalendar Le mois à afficher.
     * @param selectedDay Le jour actuellement sélectionné (pour l'état visuel).
     * @param statusProvider Une fonction suspendue qui renvoie le statut (Total, Pris) pour une date donnée.
     */
    suspend fun generateCalendarDays(
        currentCalendar: Calendar,
        selectedDay: CalendarDay?,
        statusProvider: suspend (Date) -> Pair<Int, Int>
    ): List<CalendarDay> {
        val days = mutableListOf<CalendarDay>()
        val calendar = currentCalendar.clone() as Calendar

        calendar[Calendar.DAY_OF_MONTH] = 1

        val firstDayOfWeek = calendar[Calendar.DAY_OF_WEEK]
        val offset = (firstDayOfWeek + 5) % 7

        repeat(offset) {
            days.add(CalendarDay(null, isCurrentMonth = false))
        }

        val daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
        val today = Calendar.getInstance()

        val todayZero = normalizeDate(today)

        for (dayNum in 1..daysInMonth) {
            calendar[Calendar.DAY_OF_MONTH] = dayNum
            val dateForDay = calendar.time

            val currentDayCal = normalizeDate(calendar)

            val isToday = isSameDay(calendar, today)
            val isSelected = selectedDay?.dayNumber == dayNum && selectedDay.isCurrentMonth
            val isPast = !isToday && currentDayCal.before(todayZero)

            val (total, taken) = statusProvider(dateForDay)

            val status = calculateStatus(total, taken)

            days.add(
                CalendarDay(
                    dayNumber = dayNum,
                    isToday = isToday,
                    isSelected = isSelected,
                    hasReminder = total > 0,
                    isCurrentMonth = true,
                    medicationStatus = status,
                    isPast = isPast,
                    date = dateForDay
                )
            )
        }

        val remainingDays = 42 - days.size
        repeat(remainingDays) {
            days.add(CalendarDay(null, isCurrentMonth = false))
        }

        return days
    }

    private fun calculateStatus(total: Int, taken: Int): MedicationStatus {
        return when {
            total == 0 -> MedicationStatus.NO_MEDICATION
            taken == total -> MedicationStatus.ALL_TAKEN
            taken > 0 -> MedicationStatus.PARTIAL
            else -> MedicationStatus.NONE
        }
    }

    private fun normalizeDate(cal: Calendar): Calendar {
        return (cal.clone() as Calendar).apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
    }

    private fun isSameDay(cal1: Calendar, cal2: Calendar): Boolean {
        return cal1[Calendar.YEAR] == cal2[Calendar.YEAR] &&
                cal1[Calendar.MONTH] == cal2[Calendar.MONTH] &&
                cal1[Calendar.DAY_OF_MONTH] == cal2[Calendar.DAY_OF_MONTH]
    }
}