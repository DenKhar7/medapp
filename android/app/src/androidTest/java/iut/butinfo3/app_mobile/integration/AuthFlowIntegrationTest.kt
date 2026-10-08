package iut.butinfo3.app_mobile.integration

import android.database.sqlite.SQLiteConstraintException
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import iut.butinfo3.app_mobile.model.dao.UserDao
import iut.butinfo3.app_mobile.model.database.AppDatabase
import iut.butinfo3.app_mobile.model.repository.AuthRepository
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Tests d'intégration du flux d'authentification complet.
 * Utilise Room in-memory + SecurityUtils réel + AuthRepository réel.
 */
@RunWith(AndroidJUnit4::class)
class AuthFlowIntegrationTest {

    private lateinit var db: AppDatabase
    private lateinit var userDao: UserDao
    private lateinit var authRepository: AuthRepository

    @Before
    fun setup() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).allowMainThreadQueries().build()
        userDao = db.userDao()
        authRepository = AuthRepository(
            ApplicationProvider.getApplicationContext(),
            userDao
        )
    }

    @After
    fun tearDown() {
        db.close()
        authRepository.clearActiveUser()
    }

    @Test
    fun register_thenLogin_succeeds() = runTest {
        authRepository.registerUser("Jean", "jean@test.com", "Password1!")

        val user = authRepository.loginUser("jean@test.com", "Password1!")

        assertNotNull(user)
        assertEquals("Jean", user!!.username)
        assertEquals("jean@test.com", user.email)
    }

    @Test
    fun register_thenLoginWrongPassword_returnsNull() = runTest {
        authRepository.registerUser("Jean", "jean@test.com", "Password1!")

        val user = authRepository.loginUser("jean@test.com", "WrongPass1!")

        assertNull(user)
    }

    @Test(expected = SQLiteConstraintException::class)
    fun register_duplicateEmail_throwsException() = runTest {
        authRepository.registerUser("Jean1", "same@test.com", "Password1!")
        authRepository.registerUser("Jean2", "same@test.com", "Password2!")
    }

    @Test
    fun saveActiveUserId_thenGetActiveUserId_roundTrip() {
        authRepository.saveActiveUserId(42)
        assertEquals(42, authRepository.getActiveUserId())
    }

    @Test
    fun clearActiveUser_thenGetActiveUserId_returnsNull() {
        authRepository.saveActiveUserId(42)
        authRepository.clearActiveUser()
        assertNull(authRepository.getActiveUserId())
    }
}
