package iut.butinfo3.app_mobile.integration

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import iut.butinfo3.app_mobile.TestDataFactory
import iut.butinfo3.app_mobile.model.dao.MedicamentDao
import iut.butinfo3.app_mobile.model.dao.TreatmentDao
import iut.butinfo3.app_mobile.model.dao.UserDao
import iut.butinfo3.app_mobile.model.database.AppDatabase
import iut.butinfo3.app_mobile.model.entity.TreatmentHistory
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Calendar
import java.util.Date

@RunWith(AndroidJUnit4::class)
class TreatmentDaoIntegrationTest {

    private lateinit var db: AppDatabase
    private lateinit var treatmentDao: TreatmentDao
    private lateinit var userDao: UserDao
    private lateinit var medicamentDao: MedicamentDao
    private var userId: Int = 0

    @Before
    fun setup() = runTest {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).allowMainThreadQueries().build()
        treatmentDao = db.treatmentDao()
        userDao = db.userDao()
        medicamentDao = db.medicamentDao()

        // Seed required FK data
        userId = userDao.insertUser(TestDataFactory.createUser(username = "Test", email = "t@t.com")).toInt()
        medicamentDao.insert(TestDataFactory.createSpecialite())
        medicamentDao.insert(TestDataFactory.createPresentation())
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun addTreatmentWithReminders_returnsRemindersWithIds() = runTest {
        val treatment = TestDataFactory.createTreatment(userId = userId)
        val reminders = listOf(
            TestDataFactory.createReminder(timeOfDay = "08:00"),
            TestDataFactory.createReminder(timeOfDay = "20:00")
        )

        val result = treatmentDao.addTreatmentWithReminders(treatment, reminders)

        assertEquals(2, result.size)
        assertTrue(result.all { it.id > 0 })
        assertEquals("08:00", result[0].timeOfDay)
        assertEquals("20:00", result[1].timeOfDay)
    }

    @Test
    fun getUserTreatments_returnsFlowOfTreatments() = runTest {
        val treatment = TestDataFactory.createTreatment(userId = userId)
        val reminders = listOf(TestDataFactory.createReminder())
        treatmentDao.addTreatmentWithReminders(treatment, reminders)

        val treatments = treatmentDao.getUserTreatments(userId).first()

        assertEquals(1, treatments.size)
        assertEquals("DOLIPRANE", treatments[0].medicament.nomSpecialite)
    }

    @Test
    fun deleteTreatment_cascadeDeletesReminders() = runTest {
        val treatment = TestDataFactory.createTreatment(userId = userId)
        val reminders = listOf(TestDataFactory.createReminder())
        val savedReminders = treatmentDao.addTreatmentWithReminders(treatment, reminders)
        val treatmentId = savedReminders[0].treatmentId.toLong()

        // Get the actual treatment to delete it
        val savedTreatment = treatmentDao.getTreatmentById(treatmentId)!!
        treatmentDao.deleteTreatment(savedTreatment)

        val remainingReminders = treatmentDao.getRemindersForTreatment(treatmentId)
        assertTrue(remainingReminders.isEmpty())
    }

    @Test
    fun updateTreatmentWithReminders_replacesOldReminders() = runTest {
        val treatment = TestDataFactory.createTreatment(userId = userId)
        val oldReminders = listOf(TestDataFactory.createReminder(timeOfDay = "08:00"))
        val saved = treatmentDao.addTreatmentWithReminders(treatment, oldReminders)
        val treatmentId = saved[0].treatmentId

        val savedTreatment = treatmentDao.getTreatmentById(treatmentId.toLong())!!
        val newReminders = listOf(
            TestDataFactory.createReminder(treatmentId = treatmentId, timeOfDay = "10:00"),
            TestDataFactory.createReminder(treatmentId = treatmentId, timeOfDay = "18:00")
        )
        val updated = treatmentDao.updateTreatmentWithReminders(savedTreatment, newReminders)

        assertEquals(2, updated.size)
        val allReminders = treatmentDao.getRemindersForTreatment(treatmentId.toLong())
        assertEquals(2, allReminders.size)
    }

    @Test
    fun insertHistory_thenGetHistoryForDate() = runTest {
        val treatment = TestDataFactory.createTreatment(userId = userId)
        val reminders = listOf(TestDataFactory.createReminder())
        val saved = treatmentDao.addTreatmentWithReminders(treatment, reminders)
        val reminderId = saved[0].id.toInt()

        val today = getStartOfDay()
        val history = TreatmentHistory(
            reminderId = reminderId, dateTaken = today, takenAt = Date()
        )
        treatmentDao.insertHistory(history)

        val result = treatmentDao.getHistoryForDate(today).first()
        assertEquals(1, result.size)
        assertEquals(reminderId, result[0].reminderId)
    }

    @Test
    fun deleteHistory_removesCorrectEntry() = runTest {
        val treatment = TestDataFactory.createTreatment(userId = userId)
        val reminders = listOf(TestDataFactory.createReminder())
        val saved = treatmentDao.addTreatmentWithReminders(treatment, reminders)
        val reminderId = saved[0].id.toInt()

        val today = getStartOfDay()
        val history = TreatmentHistory(
            reminderId = reminderId, dateTaken = today, takenAt = Date()
        )
        treatmentDao.insertHistory(history)
        treatmentDao.deleteHistory(reminderId, today)

        val result = treatmentDao.getHistoryForDate(today).first()
        assertTrue(result.isEmpty())
    }

    @Test
    fun getActiveReminders_filtersbyDate() = runTest {
        // Active treatment (starts today, no end)
        val activeTreatment = TestDataFactory.createTreatment(userId = userId, startDate = Date())
        treatmentDao.addTreatmentWithReminders(activeTreatment, listOf(TestDataFactory.createReminder()))

        val endOfToday = getEndOfDay()
        val result = treatmentDao.getActiveReminders(userId, endOfToday).first()

        assertTrue(result.isNotEmpty())
    }

    private fun getStartOfDay(): Date {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.time
    }

    private fun getEndOfDay(): Date {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 23)
        cal.set(Calendar.MINUTE, 59)
        cal.set(Calendar.SECOND, 59)
        cal.set(Calendar.MILLISECOND, 999)
        return cal.time
    }
}
