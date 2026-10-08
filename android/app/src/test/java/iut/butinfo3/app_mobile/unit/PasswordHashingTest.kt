package iut.butinfo3.app_mobile.unit

import android.util.Base64
import iut.butinfo3.app_mobile.utils.SecurityUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/**
 * Format de hachage v2 (PBKDF2-HMAC-SHA256, 600 000 itérations) et compatibilité avec les anciennes
 * empreintes (PBKDF2-HMAC-SHA1, 65 536 itérations, base64 brut).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [31])
class PasswordHashingTest {

    private lateinit var security: SecurityUtils

    @Before
    fun setup() {
        security = SecurityUtils()
    }

    /** Reproduit exactement l'empreinte produite par les versions précédentes de l'application. */
    private fun legacyHash(password: String, salt: String): String {
        val spec = PBEKeySpec(password.toCharArray(), Base64.decode(salt, Base64.NO_WRAP), 65536, 128)
        val bytes = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1").generateSecret(spec).encoded
        return Base64.encodeToString(bytes, Base64.NO_WRAP)
    }

    @Test
    fun hashPassword_usesV2FormatWithIterationCount() {
        val hash = security.hashPassword("Password1!", security.generateSalt())

        assertTrue("préfixe v2 attendu : $hash", hash.startsWith("v2\$600000\$"))
    }

    @Test
    fun hashPassword_isNotTheLegacyHash() {
        val salt = security.generateSalt()

        assertNotEquals(legacyHash("Password1!", salt), security.hashPassword("Password1!", salt))
    }

    @Test
    fun verifyPassword_v2_acceptsRightAndRejectsWrongPassword() {
        val salt = security.generateSalt()
        val hash = security.hashPassword("MySecure1!", salt)

        assertTrue(security.verifyPassword("MySecure1!", salt, hash))
        assertFalse(security.verifyPassword("mySecure1!", salt, hash))
        assertFalse(security.verifyPassword("", salt, hash))
    }

    @Test
    fun verifyPassword_stillAcceptsLegacyHashes() {
        val salt = security.generateSalt()
        val legacy = legacyHash("OldPass1!", salt)

        assertTrue(security.verifyPassword("OldPass1!", salt, legacy))
        assertFalse(security.verifyPassword("WrongPass1!", salt, legacy))
    }

    @Test
    fun verifyPassword_withMalformedV2Hash_returnsFalseInsteadOfCrashing() {
        val salt = security.generateSalt()

        assertFalse(security.verifyPassword("x", salt, "v2\$notanumber\$abc"))
        assertFalse(security.verifyPassword("x", salt, "v2\$"))
    }

    @Test
    fun needsRehash_isTrueForLegacyAndFalseForCurrentFormat() {
        val salt = security.generateSalt()

        assertTrue(security.needsRehash(legacyHash("OldPass1!", salt)))
        assertFalse(security.needsRehash(security.hashPassword("NewPass1!", salt)))
    }

    @Test
    fun needsRehash_isTrueWhenIterationCountIsBelowCurrent() {
        assertTrue(security.needsRehash("v2\$1000\$AAAA"))
        assertTrue(security.needsRehash("v2\$garbage\$AAAA"))
    }

    @Test
    fun sameSaltAndPassword_giveSameHash_differentSalt_giveDifferentHash() {
        val salt1 = security.generateSalt()
        val salt2 = security.generateSalt()

        assertEquals(security.hashPassword("Password1!", salt1), security.hashPassword("Password1!", salt1))
        assertNotEquals(security.hashPassword("Password1!", salt1), security.hashPassword("Password1!", salt2))
    }
}
