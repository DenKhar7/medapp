package iut.butinfo3.app_mobile.unit.repository

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import iut.butinfo3.app_mobile.InMemoryActiveUserStore
import iut.butinfo3.app_mobile.model.dao.UserDao
import iut.butinfo3.app_mobile.model.entity.User
import iut.butinfo3.app_mobile.model.repository.AuthRepository
import iut.butinfo3.app_mobile.utils.SecurityUtils
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Tests unitaires pour AuthRepository.
 * Utilise Robolectric pour android.util.Base64 ; la session est stockée en mémoire (pas de Keystore en test).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [31])
class AuthRepositoryTest {

    private lateinit var userDao: UserDao
    private lateinit var context: Context
    private lateinit var repository: AuthRepository

    @Before
    fun setup() {
        userDao = mockk(relaxed = true)
        context = ApplicationProvider.getApplicationContext()
        repository = AuthRepository(context, userDao, InMemoryActiveUserStore())
    }

    @Test
    fun registerUser_hashesPasswordAndInsertsUser() = runTest {
        coEvery { userDao.insertUser(any()) } returns 1L

        repository.registerUser("TestUser", "test@example.com", "Password1!")

        coVerify {
            userDao.insertUser(match { user ->
                user.username == "TestUser" &&
                user.email == "test@example.com" &&
                user.passwordHash != null &&
                user.passwordSalt != null &&
                user.passwordHash != "Password1!" // hash != plain text
            })
        }
    }

    @Test
    fun loginUser_validCredentials_returnsUser() = runTest {
        val securityUtils = SecurityUtils()
        val salt = securityUtils.generateSalt()
        val hash = securityUtils.hashPassword("Password1!", salt)
        val user = User(
            id = 1, username = "TestUser", email = "test@example.com",
            passwordHash = hash, passwordSalt = salt, isActive = true
        )
        coEvery { userDao.getUserByEmail("test@example.com") } returns user

        val result = repository.loginUser("test@example.com", "Password1!")

        assertNotNull(result)
        assertEquals(1, result!!.id)
    }

    @Test
    fun loginUser_wrongPassword_returnsNull() = runTest {
        val securityUtils = SecurityUtils()
        val salt = securityUtils.generateSalt()
        val hash = securityUtils.hashPassword("Password1!", salt)
        val user = User(
            id = 1, username = "TestUser", email = "test@example.com",
            passwordHash = hash, passwordSalt = salt, isActive = true
        )
        coEvery { userDao.getUserByEmail("test@example.com") } returns user

        val result = repository.loginUser("test@example.com", "WrongPass1!")

        assertNull(result)
    }

    @Test
    fun loginUser_emailNotFound_returnsNull() = runTest {
        coEvery { userDao.getUserByEmail("unknown@example.com") } returns null

        val result = repository.loginUser("unknown@example.com", "Password1!")

        assertNull(result)
    }

    @Test
    fun loginUser_inactiveUser_returnsNull() = runTest {
        val securityUtils = SecurityUtils()
        val salt = securityUtils.generateSalt()
        val hash = securityUtils.hashPassword("Password1!", salt)
        val user = User(
            id = 1, username = "TestUser", email = "test@example.com",
            passwordHash = hash, passwordSalt = salt, isActive = false
        )
        coEvery { userDao.getUserByEmail("test@example.com") } returns user

        val result = repository.loginUser("test@example.com", "Password1!")

        assertNull(result)
    }

    @Test
    fun loginUser_nullHash_returnsNull() = runTest {
        val user = User(
            id = 1, username = "TestUser", email = "test@example.com",
            passwordHash = null, passwordSalt = "somesalt", isActive = true
        )
        coEvery { userDao.getUserByEmail("test@example.com") } returns user

        val result = repository.loginUser("test@example.com", "Password1!")

        assertNull(result)
    }

    @Test
    fun loginUser_nullSalt_returnsNull() = runTest {
        val user = User(
            id = 1, username = "TestUser", email = "test@example.com",
            passwordHash = "somehash", passwordSalt = null, isActive = true
        )
        coEvery { userDao.getUserByEmail("test@example.com") } returns user

        val result = repository.loginUser("test@example.com", "Password1!")

        assertNull(result)
    }

    @Test
    fun saveActiveUserId_storesId() {
        repository.saveActiveUserId(42)
        assertEquals(42, repository.getActiveUserId())
    }

    @Test
    fun getActiveUserId_returnsStoredId() {
        repository.saveActiveUserId(99)
        assertEquals(99, repository.getActiveUserId())
    }

    @Test
    fun clearActiveUser_removesId() {
        repository.saveActiveUserId(42)
        repository.clearActiveUser()
        assertNull(repository.getActiveUserId())
    }
}
