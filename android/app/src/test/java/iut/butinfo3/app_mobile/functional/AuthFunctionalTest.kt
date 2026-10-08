package iut.butinfo3.app_mobile.functional

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import java.util.regex.Pattern

/**
 * Tests fonctionnels d'authentification avec analyse partitionnelle et limites.
 *
 * Note: ValidateText.checkEmailValid utilise android.util.Patterns et ne peut pas
 * être testé en JUnit5 pur. Les tests email sont réalisés via regex directe.
 * Les tests password et name utilisent la même logique que ValidateText mais en pur Java.
 */
class AuthFunctionalTest {

    // --- Password validation (analyse partitionnelle + limites) ---

    private fun checkPSWValid(pwd: String): Boolean {
        if (pwd.length < 8) return false
        val pattern = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[\\W_]).{8,}\$"
        return Pattern.matches(pattern, pwd)
    }

    @ParameterizedTest(name = "Password \"{0}\" -> valid={1}")
    @CsvSource(
        "'', false",            // Vide
        "Ab1!xyz, false",       // 7 chars (limite - 1)
        "Ab1!xyzw, true",       // 8 chars (limite exacte)
        "ab1!xyzw, false",      // Sans majuscule
        "AB1!XYZW, false",      // Sans minuscule
        "Abc!xyzw, false",      // Sans chiffre
        "Ab1cxyzw, false"       // Sans caractère spécial
    )
    fun passwordValidation(input: String, expected: Boolean) {
        assertEquals(expected, checkPSWValid(input))
    }

    // --- Name validation (analyse partitionnelle + limites) ---

    private fun checkNameValid(name: String): Boolean {
        if (name.isEmpty()) return false
        if (name.trim().length < 2) return false
        val pattern = "^[a-zA-ZÀ-ÿ\\s'-]+\$"
        return Pattern.matches(pattern, name.trim())
    }

    @ParameterizedTest(name = "Name \"{0}\" -> valid={1}")
    @CsvSource(
        "'', false",                // Vide
        "J, false",                 // 1 char (limite - 1)
        "Jo, true",                 // 2 chars (limite exacte)
        "Jean-Pierre, true",        // Avec tiret
        "Jean123, false",           // Avec chiffres
        "O'Brien, true"             // Avec apostrophe
    )
    fun nameValidation(input: String, expected: Boolean) {
        assertEquals(expected, checkNameValid(input))
    }

    // --- Email validation (analyse partitionnelle) ---

    private fun checkEmailValid(email: String): Boolean {
        if (email.isEmpty()) return false
        // Simplified email regex for pure JVM testing
        val pattern = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}\$"
        return Pattern.matches(pattern, email)
    }

    @ParameterizedTest(name = "Email \"{0}\" -> valid={1}")
    @CsvSource(
        "'', false",                      // Vide
        "userexample.com, false",         // Sans @
        "user@, false",                   // Sans domaine
        "user@example.com, true"          // Valide
    )
    fun emailValidation(input: String, expected: Boolean) {
        assertEquals(expected, checkEmailValid(input))
    }
}
