package iut.butinfo3.app_mobile.functional

import iut.butinfo3.app_mobile.model.DailyIntake
import iut.butinfo3.app_mobile.model.dao.TreatmentDao
import iut.butinfo3.app_mobile.model.entity.RecurrenceUnit
import iut.butinfo3.app_mobile.model.repository.TreatmentRepository
import iut.butinfo3.app_mobile.utils.RecurrenceUtils
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import java.util.Calendar
import java.util.Date

class TreatmentFunctionalTest {

    private lateinit var dao: TreatmentDao
    private lateinit var repository: TreatmentRepository

    private fun daysAgo(n: Int): Date {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DATE, -n)
        return cal.time
    }

    @BeforeEach
    fun setup() {
        dao = mockk(relaxed = true)
        repository = TreatmentRepository(dao)
    }

    // --- Récurrence quotidienne (analyse partitionnelle + limites) ---

    @ParameterizedTest(name = "daysFromStart={0}, interval={1} -> due={2}")
    @CsvSource(
        "0, 1, true",    // Jour 0, quotidien -> dû
        "1, 2, false",   // Jour 1, tous les 2 jours -> pas dû
        "2, 2, true",    // Jour 2, tous les 2 jours -> dû
        "-1, 1, false"   // Jour -1 (futur) -> pas dû
    )
    fun dailyRecurrence(daysFromStart: Int, interval: Int, expected: Boolean) {
        val startDate = if (daysFromStart >= 0) daysAgo(daysFromStart) else {
            val cal = Calendar.getInstance()
            cal.add(Calendar.DATE, -daysFromStart) // negative daysFromStart = future
            cal.time
        }
        assertEquals(expected, RecurrenceUtils.isReminderDueToday(startDate, interval, RecurrenceUnit.DAY))
    }

    // --- setMedicamentTaken partitionnement ---

    @ParameterizedTest(name = "isTaken={0} -> inserts={1}, deletes={2}")
    @CsvSource(
        "true, 1, 0",
        "false, 0, 1"
    )
    fun setMedicamentTaken_partitions(isTaken: Boolean, expectedInserts: Int, expectedDeletes: Int) = runTest {
        val intake = DailyIntake(
            reminderId = 1, medicamentName = "Doliprane", medicamentCip = "123",
            dosage = "1g", time = "08:00", isTaken = !isTaken, treatmentId = 1,
            recurrenceUnit = RecurrenceUnit.DAY, recurrenceInterval = 1, startDate = Date()
        )

        repository.setMedicamentTaken(intake, isTaken)

        coVerify(exactly = expectedInserts) { dao.insertHistory(any()) }
        coVerify(exactly = expectedDeletes) { dao.deleteHistory(any(), any()) }
    }

    // --- Limites dates traitement ---

    @ParameterizedTest(name = "startDaysAgo={0} -> due today={1}")
    @CsvSource(
        "0, true",    // startDate = today -> inclus
        "-1, false"   // startDate = tomorrow -> exclu (futur)
    )
    fun treatmentStartDateBoundary(startDaysAgo: Int, expected: Boolean) {
        val startDate = if (startDaysAgo >= 0) daysAgo(startDaysAgo) else {
            val cal = Calendar.getInstance()
            cal.add(Calendar.DATE, -startDaysAgo)
            cal.time
        }
        assertEquals(expected, RecurrenceUtils.isReminderDueToday(startDate, 1, RecurrenceUnit.DAY))
    }
}
