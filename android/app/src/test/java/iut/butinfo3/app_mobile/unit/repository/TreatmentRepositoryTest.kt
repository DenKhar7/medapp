package iut.butinfo3.app_mobile.unit.repository

import app.cash.turbine.test
import iut.butinfo3.app_mobile.model.DailyIntake
import iut.butinfo3.app_mobile.model.ReminderWithSchedule
import iut.butinfo3.app_mobile.model.dao.TreatmentDao
import iut.butinfo3.app_mobile.model.entity.RecurrenceUnit
import iut.butinfo3.app_mobile.model.entity.TreatmentHistory
import iut.butinfo3.app_mobile.model.entity.TreatmentReminder
import iut.butinfo3.app_mobile.model.entity.UserTreatment
import iut.butinfo3.app_mobile.model.repository.TreatmentRepository
import io.mockk.*
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.Date

class TreatmentRepositoryTest {

    private lateinit var dao: TreatmentDao
    private lateinit var repository: TreatmentRepository

    @BeforeEach
    fun setup() {
        dao = mockk(relaxed = true)
        repository = TreatmentRepository(dao)
    }

    @Test
    fun saveTreatmentWithReminders_delegatesToDao() = runTest {
        val treatment = UserTreatment(
            id = 0, userId = 1, cipRef = "1234567890123",
            startDate = Date(), endDate = null
        )
        val reminders = listOf(
            TreatmentReminder(treatmentId = 0, timeOfDay = "08:00", doseQuantity = "1 comprimé")
        )
        val expected = reminders.map { it.copy(id = 1) }
        coEvery { dao.addTreatmentWithReminders(treatment, reminders) } returns expected

        val result = repository.saveTreatmentWithReminders(treatment, reminders)

        assertEquals(expected, result)
        coVerify { dao.addTreatmentWithReminders(treatment, reminders) }
    }

    @Test
    fun deleteTreatment_delegatesToDao() = runTest {
        val treatment = UserTreatment(
            id = 5, userId = 1, cipRef = "1234567890123",
            startDate = Date(), endDate = null
        )
        coEvery { dao.deleteTreatment(treatment) } just Runs

        repository.deleteTreatment(treatment)

        coVerify { dao.deleteTreatment(treatment) }
    }

    @Test
    fun updateTreatmentWithReminders_delegatesToDao() = runTest {
        val treatment = UserTreatment(
            id = 5, userId = 1, cipRef = "1234567890123",
            startDate = Date(), endDate = null
        )
        val reminders = listOf(
            TreatmentReminder(id = 10, treatmentId = 5, timeOfDay = "09:00", doseQuantity = "2 comprimés")
        )
        val expectedReminders = reminders.map { it.copy(id = 0, treatmentId = 5) }
        val returnedReminders = expectedReminders.map { it.copy(id = 20) }
        coEvery { dao.updateTreatmentWithReminders(treatment, expectedReminders) } returns returnedReminders

        val result = repository.updateTreatmentWithReminders(treatment, reminders)

        assertEquals(returnedReminders, result)
        coVerify { dao.updateTreatmentWithReminders(treatment, expectedReminders) }
    }

    @Test
    fun setMedicamentTaken_whenTrue_insertHistory() = runTest {
        val intake = DailyIntake(
            reminderId = 1, medicamentName = "Doliprane", medicamentCip = "123",
            dosage = "1g", time = "08:00", isTaken = false, treatmentId = 1,
            recurrenceUnit = RecurrenceUnit.DAY, recurrenceInterval = 1, startDate = Date()
        )

        repository.setMedicamentTaken(intake, true)

        coVerify { dao.insertHistory(match { it.reminderId == 1 }) }
        coVerify(exactly = 0) { dao.deleteHistory(any(), any()) }
    }

    @Test
    fun setMedicamentTaken_whenFalse_deleteHistory() = runTest {
        val intake = DailyIntake(
            reminderId = 1, medicamentName = "Doliprane", medicamentCip = "123",
            dosage = "1g", time = "08:00", isTaken = true, treatmentId = 1,
            recurrenceUnit = RecurrenceUnit.DAY, recurrenceInterval = 1, startDate = Date()
        )

        repository.setMedicamentTaken(intake, false)

        coVerify { dao.deleteHistory(eq(1), any()) }
        coVerify(exactly = 0) { dao.insertHistory(any()) }
    }

    @Test
    fun getActiveRemindersFlow_combinesRemindersAndHistory() = runTest {
        val startDate = Date()
        val reminderWithSchedule = ReminderWithSchedule(
            reminderId = 1, timeOfDay = "08:00", doseQuantity = "1 comprimé",
            recurrenceUnit = "DAY", recurrenceInterval = 1,
            drugName = "Doliprane", drugCip = "123", treatmentId = 1,
            startDate = startDate
        )
        val history = TreatmentHistory(
            id = 1, reminderId = 1, dateTaken = Date(), takenAt = Date()
        )

        every { dao.getActiveReminders(eq(1), any()) } returns flowOf(listOf(reminderWithSchedule))
        every { dao.getHistoryForDate(any()) } returns flowOf(listOf(history))

        repository.getActiveRemindersFlow(1).test {
            val items = awaitItem()
            assertEquals(1, items.size)
            assertEquals("Doliprane", items[0].medicamentName)
            assertTrue(items[0].isTaken)
            cancelAndConsumeRemainingEvents()
        }
    }
}
