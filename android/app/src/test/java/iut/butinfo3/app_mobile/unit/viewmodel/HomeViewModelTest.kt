package iut.butinfo3.app_mobile.unit.viewmodel

import app.cash.turbine.test
import iut.butinfo3.app_mobile.model.DailyIntake
import iut.butinfo3.app_mobile.model.entity.RecurrenceUnit
import iut.butinfo3.app_mobile.model.entity.User
import iut.butinfo3.app_mobile.model.repository.SessionRepository
import iut.butinfo3.app_mobile.model.repository.TreatmentRepository
import iut.butinfo3.app_mobile.view_model.HomeViewModel
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.*
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.Date

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

    private lateinit var treatmentRepository: TreatmentRepository
    private val testDispatcher = UnconfinedTestDispatcher()
    private val userFlow = MutableStateFlow<User?>(null)

    @BeforeEach
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        treatmentRepository = mockk(relaxed = true)
        mockkObject(SessionRepository)
        every { SessionRepository.selectedUser } returns userFlow
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkObject(SessionRepository)
    }

    @Test
    fun dailyIntakes_emptyWhenNoUser() = runTest {
        val viewModel = HomeViewModel(treatmentRepository, SessionRepository)

        viewModel.dailyIntakes.test {
            assertEquals(emptyList<DailyIntake>(), awaitItem())
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun dailyIntakes_populatedWhenUserSelected() = runTest {
        val intake = DailyIntake(
            reminderId = 1, medicamentName = "Doliprane", medicamentCip = "123",
            dosage = "1g", time = "08:00", isTaken = false, treatmentId = 1,
            recurrenceUnit = RecurrenceUnit.DAY, recurrenceInterval = 1,
            startDate = Date() // today -> due today
        )
        every { treatmentRepository.getActiveRemindersFlow(42) } returns flowOf(listOf(intake))
        coEvery { treatmentRepository.getMedicationStatusForDateSync(any(), any()) } returns Pair(0, 0)

        val viewModel = HomeViewModel(treatmentRepository, SessionRepository)
        val user = User(id = 42, username = "Test", email = "test@test.com")
        userFlow.value = user

        // Give a moment for collection
        advanceUntilIdle()

        val result = viewModel.dailyIntakes.value
        assertEquals(1, result.size)
        assertEquals("Doliprane", result[0].medicamentName)
    }

    @Test
    fun toggleTaken_delegatesToRepository() = runTest {
        val viewModel = HomeViewModel(treatmentRepository, SessionRepository)
        val intake = DailyIntake(
            reminderId = 1, medicamentName = "Doliprane", medicamentCip = "123",
            dosage = "1g", time = "08:00", isTaken = false, treatmentId = 1,
            recurrenceUnit = RecurrenceUnit.DAY, recurrenceInterval = 1, startDate = Date()
        )

        viewModel.toggleTaken(intake, true)
        advanceUntilIdle()

        coVerify { treatmentRepository.setMedicamentTaken(intake, true) }
    }
}
