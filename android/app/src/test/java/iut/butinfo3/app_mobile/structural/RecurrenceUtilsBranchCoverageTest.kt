package iut.butinfo3.app_mobile.structural

import iut.butinfo3.app_mobile.model.entity.RecurrenceUnit
import iut.butinfo3.app_mobile.utils.RecurrenceUtils
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.util.Calendar
import java.util.Date

/**
 * Tests structurels pour RecurrenceUtils - couverture de toutes les branches.
 */
class RecurrenceUtilsBranchCoverageTest {

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

    private fun getCurrentDayOfWeek(): Int =
        Calendar.getInstance().get(Calendar.DAY_OF_WEEK)

    private fun recurrenceUnitForCalendarDay(calendarDay: Int): RecurrenceUnit = when (calendarDay) {
        Calendar.MONDAY -> RecurrenceUnit.MONDAY
        Calendar.TUESDAY -> RecurrenceUnit.TUESDAY
        Calendar.WEDNESDAY -> RecurrenceUnit.WEDNESDAY
        Calendar.THURSDAY -> RecurrenceUnit.THURSDAY
        Calendar.FRIDAY -> RecurrenceUnit.FRIDAY
        Calendar.SATURDAY -> RecurrenceUnit.SATURDAY
        Calendar.SUNDAY -> RecurrenceUnit.SUNDAY
        else -> RecurrenceUnit.DAY
    }

    private fun nextCalendarDay(calendarDay: Int): Int =
        if (calendarDay == Calendar.SATURDAY) Calendar.SUNDAY else calendarDay + 1

    // --- B1: today.before(startOfDay) -> false ---
    @Test
    fun b1_futureStartDate_returnsFalse() {
        assertFalse(RecurrenceUtils.isReminderDueToday(daysFromNow(1)))
    }

    // --- B2: unit == DAY, diffDays % interval == 0 -> true ---
    @Test
    fun b2_dayUnit_intervalMatch_returnsTrue() {
        assertTrue(RecurrenceUtils.isReminderDueToday(daysAgo(4), 2, RecurrenceUnit.DAY))
    }

    // --- B3: unit == DAY, diffDays % interval != 0 -> false ---
    @Test
    fun b3_dayUnit_intervalMismatch_returnsFalse() {
        assertFalse(RecurrenceUtils.isReminderDueToday(daysAgo(3), 2, RecurrenceUnit.DAY))
    }

    // --- B4: weekday unit, wrong day of week -> false ---
    @Test
    fun b4_weekdayUnit_wrongDay_returnsFalse() {
        val todayCalDay = getCurrentDayOfWeek()
        val tomorrowCalDay = nextCalendarDay(todayCalDay)
        val unit = recurrenceUnitForCalendarDay(tomorrowCalDay)
        assertFalse(RecurrenceUtils.isReminderDueToday(Date(), 1, unit))
    }

    // --- B5: weekday unit, correct day, correct week -> true ---
    @Test
    fun b5_weekdayUnit_correctDayAndWeek_returnsTrue() {
        val todayCalDay = getCurrentDayOfWeek()
        val unit = recurrenceUnitForCalendarDay(todayCalDay)
        // Started 14 days ago, interval 2 -> 14/7=2, 2%2=0 -> true
        assertTrue(RecurrenceUtils.isReminderDueToday(daysAgo(14), 2, unit))
    }

    // --- B6: weekday unit, correct day, wrong week -> false ---
    @Test
    fun b6_weekdayUnit_correctDayWrongWeek_returnsFalse() {
        val todayCalDay = getCurrentDayOfWeek()
        val unit = recurrenceUnitForCalendarDay(todayCalDay)
        // Started 7 days ago, interval 2 -> 7/7=1, 1%2=1 -> false
        assertFalse(RecurrenceUtils.isReminderDueToday(daysAgo(7), 2, unit))
    }

    // --- getCalendarDayConstant: test each RecurrenceUnit value ---

    @Test
    fun dayUnit_startToday_interval1_returnsTrue() {
        // DAY branch: getCalendarDayConstant returns -1, enters DAY branch
        assertTrue(RecurrenceUtils.isReminderDueToday(Date(), 1, RecurrenceUnit.DAY))
    }

    @Test
    fun mondayUnit_onMonday_returnsTrue() {
        testSpecificWeekday(RecurrenceUnit.MONDAY, Calendar.MONDAY)
    }

    @Test
    fun tuesdayUnit_onTuesday_returnsTrue() {
        testSpecificWeekday(RecurrenceUnit.TUESDAY, Calendar.TUESDAY)
    }

    @Test
    fun wednesdayUnit_onWednesday_returnsTrue() {
        testSpecificWeekday(RecurrenceUnit.WEDNESDAY, Calendar.WEDNESDAY)
    }

    @Test
    fun thursdayUnit_onThursday_returnsTrue() {
        testSpecificWeekday(RecurrenceUnit.THURSDAY, Calendar.THURSDAY)
    }

    @Test
    fun fridayUnit_onFriday_returnsTrue() {
        testSpecificWeekday(RecurrenceUnit.FRIDAY, Calendar.FRIDAY)
    }

    @Test
    fun saturdayUnit_onSaturday_returnsTrue() {
        testSpecificWeekday(RecurrenceUnit.SATURDAY, Calendar.SATURDAY)
    }

    @Test
    fun sundayUnit_onSunday_returnsTrue() {
        testSpecificWeekday(RecurrenceUnit.SUNDAY, Calendar.SUNDAY)
    }

    /**
     * Tests that when the current day matches the unit, the result is true.
     * When it doesn't match, the result is false.
     * We always exercise the branch to ensure it's covered.
     */
    private fun testSpecificWeekday(unit: RecurrenceUnit, expectedCalendarDay: Int) {
        val todayCalDay = getCurrentDayOfWeek()
        val startDate = Date()

        if (todayCalDay == expectedCalendarDay) {
            assertTrue(RecurrenceUtils.isReminderDueToday(startDate, 1, unit))
        } else {
            assertFalse(RecurrenceUtils.isReminderDueToday(startDate, 1, unit))
        }
    }
}
