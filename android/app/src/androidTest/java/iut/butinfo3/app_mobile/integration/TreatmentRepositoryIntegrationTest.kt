package iut.butinfo3.app_mobile.integration

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import iut.butinfo3.app_mobile.TestDataFactory
import iut.butinfo3.app_mobile.model.dao.MedicamentDao
import iut.butinfo3.app_mobile.model.dao.TreatmentDao
import iut.butinfo3.app_mobile.model.dao.UserDao
import iut.butinfo3.app_mobile.model.database.AppDatabase
import iut.butinfo3.app_mobile.model.repository.TreatmentRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TreatmentRepositoryIntegrationTest {

    private lateinit var db: AppDatabase
    private lateinit var repository: TreatmentRepository
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
        repository = TreatmentRepository(treatmentDao)

        // Seed FK data
        userId = userDao.insertUser(TestDataFactory.createUser(username = "Test", email = "t@t.com")).toInt()
        medicamentDao.insert(TestDataFactory.createSpecialite())
        medicamentDao.insert(TestDataFactory.createPresentation())
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun saveTreatment_thenGetActiveRemindersFlow_emitsDailyIntakes() = runTest {
        val treatment = TestDataFactory.createTreatment(userId = userId)
        val reminders = listOf(TestDataFactory.createReminder(timeOfDay = "08:00"))

        repository.saveTreatmentWithReminders(treatment, reminders)

        val intakes = repository.getActiveRemindersFlow(userId).first()
        assertTrue(intakes.isNotEmpty())
        assertEquals("DOLIPRANE", intakes[0].medicamentName)
    }

    @Test
    fun setMedicamentTaken_true_thenFalse_togglesHistory() = runTest {
        val treatment = TestDataFactory.createTreatment(userId = userId)
        val reminders = listOf(TestDataFactory.createReminder())
        val savedReminders = repository.saveTreatmentWithReminders(treatment, reminders)

        val intakes = repository.getActiveRemindersFlow(userId).first()
        assertFalse(intakes[0].isTaken)

        // Mark as taken
        repository.setMedicamentTaken(intakes[0], true)
        val afterTaken = repository.getActiveRemindersFlow(userId).first()
        assertTrue(afterTaken[0].isTaken)

        // Mark as not taken
        repository.setMedicamentTaken(afterTaken[0], false)
        val afterUntaken = repository.getActiveRemindersFlow(userId).first()
        assertFalse(afterUntaken[0].isTaken)
    }

    @Test
    fun deleteTreatment_removesFromActiveFlow() = runTest {
        val treatment = TestDataFactory.createTreatment(userId = userId)
        val reminders = listOf(TestDataFactory.createReminder())
        val savedReminders = repository.saveTreatmentWithReminders(treatment, reminders)
        val treatmentId = savedReminders[0].treatmentId.toLong()

        val savedTreatment = repository.getTreatmentById(treatmentId)!!
        repository.deleteTreatment(savedTreatment)

        val intakes = repository.getActiveRemindersFlow(userId).first()
        assertTrue(intakes.isEmpty())
    }
}
