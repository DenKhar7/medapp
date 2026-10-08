package iut.butinfo3.app_mobile.unit.viewmodel

import android.database.sqlite.SQLiteConstraintException
import iut.butinfo3.app_mobile.model.repository.AuthRepository
import iut.butinfo3.app_mobile.view_model.RegisterState
import iut.butinfo3.app_mobile.view_model.RegisterViewModel
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Tests pour RegisterViewModel.
 * Utilise Robolectric car RegisterViewModel instancie ValidateText() qui dépend de android.util.Patterns.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [31])
class RegisterViewModelTest {

    private lateinit var repository: AuthRepository
    private lateinit var viewModel: RegisterViewModel
    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        repository = mockk(relaxed = true)
        viewModel = RegisterViewModel(repository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun initialState_isIdle() {
        assertEquals(RegisterState.Idle, viewModel.registerState.value)
    }

    @Test
    fun onRegister_passwordsDontMatch_emitsWrongConfirmPassword() = runTest {
        viewModel.onRegister("Jean", "Password1!", "jean@test.com", "DifferentPwd1!")
        assertEquals(RegisterState.WrongConfirmPassword, viewModel.registerState.value)
    }

    @Test
    fun onRegister_badName_emitsBadUserName() = runTest {
        viewModel.onRegister("J", "Password1!", "jean@test.com", "Password1!")
        assertEquals(RegisterState.BadUserName, viewModel.registerState.value)
    }

    @Test
    fun onRegister_badEmail_emitsBadEmail() = runTest {
        viewModel.onRegister("Jean", "Password1!", "invalid-email", "Password1!")
        assertEquals(RegisterState.BadEmail, viewModel.registerState.value)
    }

    @Test
    fun onRegister_badPassword_emitsBadPassword() = runTest {
        viewModel.onRegister("Jean", "weak", "jean@test.com", "weak")
        assertEquals(RegisterState.BadPassword, viewModel.registerState.value)
    }

    @Test
    fun onRegister_success_emitsSuccess() = runTest {
        coEvery { repository.registerUser(any(), any(), any()) } just Runs

        viewModel.onRegister("Jean", "Password1!", "jean@test.com", "Password1!")

        assertEquals(RegisterState.Success, viewModel.registerState.value)
    }

    @Test
    fun onRegister_duplicateEmail_emitsEmailAlreadyExists() = runTest {
        coEvery { repository.registerUser(any(), any(), any()) } throws SQLiteConstraintException()

        viewModel.onRegister("Jean", "Password1!", "jean@test.com", "Password1!")

        assertEquals(RegisterState.EmailAlreadyExists, viewModel.registerState.value)
    }

    @Test
    fun onRegister_genericError_emitsGenericError() = runTest {
        coEvery { repository.registerUser(any(), any(), any()) } throws RuntimeException("DB error")

        viewModel.onRegister("Jean", "Password1!", "jean@test.com", "Password1!")

        assertEquals(RegisterState.GenericError, viewModel.registerState.value)
    }
}
