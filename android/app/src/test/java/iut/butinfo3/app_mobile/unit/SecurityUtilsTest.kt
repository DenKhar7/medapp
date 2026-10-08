package iut.butinfo3.app_mobile.unit

import iut.butinfo3.app_mobile.utils.SecurityUtils
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Tests unitaires pour SecurityUtils.
 * Utilise Robolectric car SecurityUtils dépend de android.util.Base64.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [31])
class SecurityUtilsTest {

    private lateinit var securityUtils: SecurityUtils

    @Before
    fun setup() {
        securityUtils = SecurityUtils()
    }

    @Test
    fun generateSalt_returnsNonEmptyBase64String() {
        val salt = securityUtils.generateSalt()
        assertTrue(salt.isNotEmpty())
    }

    @Test
    fun generateSalt_producesTwoDifferentSalts() {
        val salt1 = securityUtils.generateSalt()
        val salt2 = securityUtils.generateSalt()
        assertNotEquals(salt1, salt2)
    }

    @Test
    fun hashPassword_sameInput_sameResult() {
        val salt = securityUtils.generateSalt()
        val hash1 = securityUtils.hashPassword("Password1!", salt)
        val hash2 = securityUtils.hashPassword("Password1!", salt)
        assertEquals(hash1, hash2)
    }

    @Test
    fun hashPassword_differentPassword_differentHash() {
        val salt = securityUtils.generateSalt()
        val hash1 = securityUtils.hashPassword("Password1!", salt)
        val hash2 = securityUtils.hashPassword("Password2!", salt)
        assertNotEquals(hash1, hash2)
    }

    @Test
    fun hashPassword_differentSalt_differentHash() {
        val salt1 = securityUtils.generateSalt()
        val salt2 = securityUtils.generateSalt()
        val hash1 = securityUtils.hashPassword("Password1!", salt1)
        val hash2 = securityUtils.hashPassword("Password1!", salt2)
        assertNotEquals(hash1, hash2)
    }

    @Test
    fun verifyPassword_correctPassword_returnsTrue() {
        val salt = securityUtils.generateSalt()
        val hash = securityUtils.hashPassword("MySecure1!", salt)
        assertTrue(securityUtils.verifyPassword("MySecure1!", salt, hash))
    }

    @Test
    fun verifyPassword_incorrectPassword_returnsFalse() {
        val salt = securityUtils.generateSalt()
        val hash = securityUtils.hashPassword("MySecure1!", salt)
        assertFalse(securityUtils.verifyPassword("WrongPass1!", salt, hash))
    }
}
