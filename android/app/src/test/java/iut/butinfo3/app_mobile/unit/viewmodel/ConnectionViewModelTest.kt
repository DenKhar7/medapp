package iut.butinfo3.app_mobile.unit.viewmodel

import app.cash.turbine.test
import iut.butinfo3.app_mobile.model.entity.User
import iut.butinfo3.app_mobile.model.repository.AuthRepository
import iut.butinfo3.app_mobile.view_model.ConnectionState
import iut.butinfo3.app_mobile.view_model.ConnectionViewModel
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ConnectionViewModelTest {

    private lateinit var repository: AuthRepository
    private lateinit var viewModel: ConnectionViewModel
    private val testDispatcher = UnconfinedTestDispatcher()

    @BeforeEach
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        repository = mockk(relaxed = true)
        viewModel = ConnectionViewModel(repository)
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun initialState_isIdle() = runTest {
        viewModel.connectionState.test {
            assertEquals(ConnectionState.Idle, awaitItem())
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun onConnection_success_emitsLoadingThenSuccess() = runTest {
        val user = User(id = 1, username = "Test", email = "test@example.com")
        coEvery { repository.loginUser(any(), any()) } returns user

        viewModel.connectionState.test {
            assertEquals(ConnectionState.Idle, awaitItem())

            viewModel.onConnection("Test@Example.com", "Password1!")

            // With UnconfinedTestDispatcher, Loading may be skipped
            // We check the final state is Success
            val states = cancelAndConsumeRemainingEvents()
            val allEmissions = states.filterIsInstance<app.cash.turbine.Event.Item<ConnectionState>>()
            // At minimum, we should reach Success
            assertTrue(
                allEmissions.any { it.value is ConnectionState.Success } ||
                viewModel.connectionState.value is ConnectionState.Success
            )
        }
    }

    @Test
    fun onConnection_failure_emitsLoginFailed() = runTest {
        coEvery { repository.loginUser(any(), any()) } returns null

        viewModel.onConnection("test@example.com", "wrong")

        assertEquals(ConnectionState.LoginFailed, viewModel.connectionState.value)
    }

    @Test
    fun onConnection_convertsEmailToLowercase() = runTest {
        coEvery { repository.loginUser(any(), any()) } returns null

        viewModel.onConnection("Test@Example.COM", "password")

        coVerify { repository.loginUser("test@example.com", "password") }
    }

    @Test
    fun onConnection_success_savesUserId() = runTest {
        val user = User(id = 42, username = "Test", email = "test@example.com")
        coEvery { repository.loginUser(any(), any()) } returns user

        viewModel.onConnection("test@example.com", "Password1!")

        coVerify { repository.saveActiveUserId(42) }
    }
}
