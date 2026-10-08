package iut.butinfo3.app_mobile.unit

import iut.butinfo3.app_mobile.model.entity.RecurrenceUnit
import iut.butinfo3.app_mobile.utils.RecurrenceUtils
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.util.Calendar
import java.util.Date

class RecurrenceUtilsTest {

    private fun today(): Date = Date()

    private fun daysAgo(n: Int): Date {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DATE, -n)
        return cal.time
    }

    private fun daysFromNow(n: Int): Date {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DATE, n)
        return cal.time
    }

    private fun getCurrentDayOfWeek(): Int {
        return Calendar.getInstance().get(Calendar.DAY_OF_WEEK)
    }

    private fun recurrenceUnitForCalendarDay(calendarDay: Int): RecurrenceUnit {
        return when (calendarDay) {
            Calendar.MONDAY -> RecurrenceUnit.MONDAY
            Calendar.TUESDAY -> RecurrenceUnit.TUESDAY
            Calendar.WEDNESDAY -> RecurrenceUnit.WEDNESDAY
            Calendar.THURSDAY -> RecurrenceUnit.THURSDAY
            Calendar.FRIDAY -> RecurrenceUnit.FRIDAY
            Calendar.SATURDAY -> RecurrenceUnit.SATURDAY
            Calendar.SUNDAY -> RecurrenceUnit.SUNDAY
            else -> RecurrenceUnit.DAY
        }
    }

    private fun nextCalendarDay(calendarDay: Int): Int {
        // Calendar days: SUN=1, MON=2, ..., SAT=7
        return if (calendarDay == Calendar.SATURDAY) Calendar.SUNDAY else calendarDay + 1
    }

    // --- Daily recurrence ---

    @Test
    fun isReminderDueToday_startToday_daily_returnsTrue() {
        assertTrue(RecurrenceUtils.isReminderDueToday(today(), 1, RecurrenceUnit.DAY))
    }

    @Test
    fun isReminderDueToday_every2Days_evenDay_returnsTrue() {
        // Started 2 days ago, every 2 days -> due today
        assertTrue(RecurrenceUtils.isReminderDueToday(daysAgo(2), 2, RecurrenceUnit.DAY))
    }

    @Test
    fun isReminderDueToday_every2Days_oddDay_returnsFalse() {
        // Started 1 day ago, every 2 days -> not due today
        assertFalse(RecurrenceUtils.isReminderDueToday(daysAgo(1), 2, RecurrenceUnit.DAY))
    }

    @Test
    fun isReminderDueToday_futureDate_returnsFalse() {
        assertFalse(RecurrenceUtils.isReminderDueToday(daysFromNow(1), 1, RecurrenceUnit.DAY))
    }

    // --- Weekday recurrence ---

    @Test
    fun isReminderDueToday_matchingWeekday_returnsTrue() {
        val todayCalDay = getCurrentDayOfWeek()
        val unit = recurrenceUnitForCalendarDay(todayCalDay)
        // Started today, interval 1 -> due today
        assertTrue(RecurrenceUtils.isReminderDueToday(today(), 1, unit))
    }

    @Test
    fun isReminderDueToday_nonMatchingWeekday_returnsFalse() {
        val todayCalDay = getCurrentDayOfWeek()
        val tomorrowCalDay = nextCalendarDay(todayCalDay)
        val unit = recurrenceUnitForCalendarDay(tomorrowCalDay)
        // The unit is tomorrow's day, so not due today
        assertFalse(RecurrenceUtils.isReminderDueToday(today(), 1, unit))
    }

    @Test
    fun isReminderDueToday_biWeekly_correctWeek_returnsTrue() {
        val todayCalDay = getCurrentDayOfWeek()
        val unit = recurrenceUnitForCalendarDay(todayCalDay)
        // Started 14 days ago (2 weeks), interval 2 -> 14/7=2 weeks, 2%2=0 -> due
        assertTrue(RecurrenceUtils.isReminderDueToday(daysAgo(14), 2, unit))
    }

    @Test
    fun isReminderDueToday_biWeekly_wrongWeek_returnsFalse() {
        val todayCalDay = getCurrentDayOfWeek()
        val unit = recurrenceUnitForCalendarDay(todayCalDay)
        // Started 7 days ago (1 week), interval 2 -> 7/7=1 week, 1%2=1 -> not due
        assertFalse(RecurrenceUtils.isReminderDueToday(daysAgo(7), 2, unit))
    }
}
