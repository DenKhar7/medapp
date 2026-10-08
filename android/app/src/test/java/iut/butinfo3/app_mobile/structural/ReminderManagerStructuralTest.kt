package iut.butinfo3.app_mobile.structural

import android.app.AlarmManager
import android.content.Context
import iut.butinfo3.app_mobile.model.entity.TreatmentReminder
import iut.butinfo3.app_mobile.reminder.ReminderManager
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowAlarmManager

/**
 * Tests structurels pour ReminderManager - couverture de branches.
 * Utilise Robolectric pour simuler AlarmManager et PendingIntent.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [31])
class ReminderManagerStructuralTest {

    private lateinit var context: Context
    private lateinit var reminderManager: ReminderManager
    private lateinit var shadowAlarmManager: ShadowAlarmManager

    @Before
    fun setup() {
        context = RuntimeEnvironment.getApplication()
        reminderManager = ReminderManager(context)
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        shadowAlarmManager = Shadows.shadowOf(alarmManager)
    }

    // --- B4: reminder.id <= 0 -> return false ---
    @Test
    fun scheduleReminder_invalidId_zero_returnsFalse() {
        val reminder = TreatmentReminder(
            id = 0, treatmentId = 1, timeOfDay = "08:00", doseQuantity = "1 comprimé"
        )
        assertFalse(reminderManager.scheduleReminder(reminder, "Doliprane", "Jean"))
    }

    @Test
    fun scheduleReminder_invalidId_negative_returnsFalse() {
        val reminder = TreatmentReminder(
            id = -1, treatmentId = 1, timeOfDay = "08:00", doseQuantity = "1 comprimé"
        )
        assertFalse(reminderManager.scheduleReminder(reminder, "Doliprane", "Jean"))
    }

    // --- B5: invalid time format -> return false ---
    @Test
    fun scheduleReminder_invalidTimeFormat_returnsFalse() {
        val reminder = TreatmentReminder(
            id = 1, treatmentId = 1, timeOfDay = "invalid", doseQuantity = "1 comprimé"
        )
        assertFalse(reminderManager.scheduleReminder(reminder, "Doliprane", "Jean"))
    }

    @Test
    fun scheduleReminder_emptyTime_returnsFalse() {
        val reminder = TreatmentReminder(
            id = 2, treatmentId = 1, timeOfDay = "", doseQuantity = "1 comprimé"
        )
        assertFalse(reminderManager.scheduleReminder(reminder, "Doliprane", "Jean"))
    }

    @Test
    fun scheduleReminder_overflowHour_returnsFalse() {
        val reminder = TreatmentReminder(
            id = 3, treatmentId = 1, timeOfDay = "24:00", doseQuantity = "1 comprimé"
        )
        assertFalse(reminderManager.scheduleReminder(reminder, "Doliprane", "Jean"))
    }

    @Test
    fun scheduleReminder_overflowMinute_returnsFalse() {
        val reminder = TreatmentReminder(
            id = 4, treatmentId = 1, timeOfDay = "12:60", doseQuantity = "1 comprimé"
        )
        assertFalse(reminderManager.scheduleReminder(reminder, "Doliprane", "Jean"))
    }

    // --- B6/B7: Valid reminder scheduling ---
    @Test
    fun scheduleReminder_validInput_returnsTrue() {
        val reminder = TreatmentReminder(
            id = 10, treatmentId = 1, timeOfDay = "08:00", doseQuantity = "1 comprimé"
        )
        val result = reminderManager.scheduleReminder(reminder, "Doliprane", "Jean")
        assertTrue(result)
    }

    @Test
    fun scheduleReminder_validLateTime_returnsTrue() {
        val reminder = TreatmentReminder(
            id = 11, treatmentId = 1, timeOfDay = "23:59", doseQuantity = "1 comprimé"
        )
        val result = reminderManager.scheduleReminder(reminder, "Doliprane", "Jean")
        assertTrue(result)
    }

    // --- B8-B10: cancelReminder branches ---
    @Test
    fun cancelReminder_invalidId_doesNotThrow() {
        // reminderId <= 0 -> early return, no exception
        reminderManager.cancelReminder(0)
        reminderManager.cancelReminder(-1)
    }

    @Test
    fun cancelReminder_validId_doesNotThrow() {
        // Even without a prior schedule, this should not throw
        reminderManager.cancelReminder(999)
    }

    @Test
    fun cancelReminder_afterSchedule_doesNotThrow() {
        val reminder = TreatmentReminder(
            id = 20, treatmentId = 1, timeOfDay = "10:00", doseQuantity = "1 comprimé"
        )
        reminderManager.scheduleReminder(reminder, "Aspirine", "Pierre")
        reminderManager.cancelReminder(20)
    }

    // --- Snooze ---
    @Test
    fun scheduleSnooze_validInput_returnsTrue() {
        val result = reminderManager.scheduleSnooze(
            reminderId = 5,
            drugName = "Doliprane",
            doseQuantity = "1 comprimé",
            userName = "Jean",
            treatmentId = 1,
            delayMinutes = 15
        )
        assertTrue(result)
    }
}
