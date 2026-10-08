package iut.butinfo3.app_mobile.unit.repository

import android.content.Context
import android.util.Base64
import androidx.test.core.app.ApplicationProvider
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import iut.butinfo3.app_mobile.InMemoryActiveUserStore
import iut.butinfo3.app_mobile.model.dao.UserDao
import iut.butinfo3.app_mobile.model.entity.User
import iut.butinfo3.app_mobile.model.repository.AuthRepository
import iut.butinfo3.app_mobile.utils.SecurityUtils
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/** Mise à niveau transparente des anciennes empreintes de mot de passe lors d'une connexion réussie. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [31])
class AuthRepositoryUpgradeTest {

    private lateinit var userDao: UserDao
    private lateinit var repository: AuthRepository
    private val security = SecurityUtils()

    @Before
    fun setup() {
        userDao = mockk(relaxed = true)
        val context: Context = ApplicationProvider.getApplicationContext()
        repository = AuthRepository(context, userDao, InMemoryActiveUserStore())
    }

    private fun legacyUser(password: String, salt: String): User {
        val spec = PBEKeySpec(password.toCharArray(), Base64.decode(salt, Base64.NO_WRAP), 65536, 128)
        val hash = Base64.encodeToString(
            SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1").generateSecret(spec).encoded,
            Base64.NO_WRAP
        )
        return User(
            id = 7, username = "Ancien", email = "old@example.com",
            passwordHash = hash, passwordSalt = salt, isActive = true
        )
    }

    @Test
    fun loginWithLegacyHash_succeedsAndStoresAnUpgradedHash() = runTest {
        val salt = security.generateSalt()
        val user = legacyUser("OldPass1!", salt)
        coEvery { userDao.getUserByEmail("old@example.com") } returns user
        val saved = slot<User>()
        coEvery { userDao.update(capture(saved)) } returns Unit

        val result = repository.loginUser("old@example.com", "OldPass1!")

        assertNotNull(result)
        coVerify(exactly = 1) { userDao.update(any()) }
        assertTrue("nouveau format attendu", saved.captured.passwordHash!!.startsWith("v2\$"))
        assertNotEquals("nouveau sel attendu", salt, saved.captured.passwordSalt)
        assertEquals("l'utilisateur renvoyé est la version mise à niveau", saved.captured, result)
        // et le mot de passe fonctionne toujours avec la nouvelle empreinte
        assertTrue(
            security.verifyPassword("OldPass1!", saved.captured.passwordSalt!!, saved.captured.passwordHash!!)
        )
    }

    @Test
    fun loginWithWrongPassword_doesNotTouchTheStoredHash() = runTest {
        val user = legacyUser("OldPass1!", security.generateSalt())
        coEvery { userDao.getUserByEmail("old@example.com") } returns user

        val result = repository.loginUser("old@example.com", "WrongPass1!")

        assertNull(result)
        coVerify(exactly = 0) { userDao.update(any()) }
    }

    @Test
    fun loginWithCurrentFormat_doesNotRewriteTheHash() = runTest {
        val salt = security.generateSalt()
        val user = User(
            id = 8, username = "Nouveau", email = "new@example.com",
            passwordHash = security.hashPassword("NewPass1!", salt), passwordSalt = salt, isActive = true
        )
        coEvery { userDao.getUserByEmail("new@example.com") } returns user

        val result = repository.loginUser("new@example.com", "NewPass1!")

        assertEquals(user, result)
        coVerify(exactly = 0) { userDao.update(any()) }
    }

    @Test
    fun newlyRegisteredAccounts_useTheCurrentFormat() = runTest {
        val inserted = slot<User>()
        coEvery { userDao.insertUser(capture(inserted)) } returns 1L

        repository.registerUser("Nouveau", "n@example.com", "Password1!")

        assertTrue(inserted.captured.passwordHash!!.startsWith("v2\$"))
        assertFalse(security.needsRehash(inserted.captured.passwordHash!!))
    }
}
