package iut.butinfo3.app_mobile.unit

import iut.butinfo3.app_mobile.utils.ValidateText
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Tests unitaires pour ValidateText.
 * Utilise Robolectric car ValidateText dépend de android.util.Patterns.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [31])
class ValidateTextTest {

    private lateinit var validate: ValidateText

    @Before
    fun setup() {
        validate = ValidateText()
    }

    // --- Email validation ---

    @Test
    fun checkEmailValid_validEmail_returnsTrue() {
        assertTrue(validate.checkEmailValid("user@example.com"))
    }

    @Test
    fun checkEmailValid_empty_returnsFalse() {
        assertFalse(validate.checkEmailValid(""))
    }

    @Test
    fun checkEmailValid_noAtSign_returnsFalse() {
        assertFalse(validate.checkEmailValid("userexample.com"))
    }

    @Test
    fun checkEmailValid_noDomain_returnsFalse() {
        assertFalse(validate.checkEmailValid("user@"))
    }

    // --- Password validation ---

    @Test
    fun checkPSWValid_validPassword_returnsTrue() {
        assertTrue(validate.checkPSWValid("Ab1!xyzw"))
    }

    @Test
    fun checkPSWValid_lessThan8Chars_returnsFalse() {
        assertFalse(validate.checkPSWValid("Ab1!xyz"))
    }

    @Test
    fun checkPSWValid_noUppercase_returnsFalse() {
        assertFalse(validate.checkPSWValid("ab1!xyzw"))
    }

    @Test
    fun checkPSWValid_noLowercase_returnsFalse() {
        assertFalse(validate.checkPSWValid("AB1!XYZW"))
    }

    @Test
    fun checkPSWValid_noDigit_returnsFalse() {
        assertFalse(validate.checkPSWValid("Abc!xyzw"))
    }

    @Test
    fun checkPSWValid_noSpecialChar_returnsFalse() {
        assertFalse(validate.checkPSWValid("Ab1cxyzw"))
    }

    // --- Name validation ---

    @Test
    fun checkNameValid_validName_returnsTrue() {
        assertTrue(validate.checkNameValid("Jean"))
    }

    @Test
    fun checkNameValid_empty_returnsFalse() {
        assertFalse(validate.checkNameValid(""))
    }

    @Test
    fun checkNameValid_oneChar_returnsFalse() {
        assertFalse(validate.checkNameValid("J"))
    }

    @Test
    fun checkNameValid_withAccents_returnsTrue() {
        assertTrue(validate.checkNameValid("Jérôme"))
    }

    @Test
    fun checkNameValid_withDigits_returnsFalse() {
        assertFalse(validate.checkNameValid("Jean123"))
    }

    @Test
    fun checkNameValid_withSpecialChars_returnsFalse() {
        assertFalse(validate.checkNameValid("Jean@!"))
    }

    @Test
    fun checkNameValid_withHyphen_returnsTrue() {
        assertTrue(validate.checkNameValid("Jean-Pierre"))
    }

    @Test
    fun checkNameValid_withApostrophe_returnsTrue() {
        assertTrue(validate.checkNameValid("O'Brien"))
    }
}
