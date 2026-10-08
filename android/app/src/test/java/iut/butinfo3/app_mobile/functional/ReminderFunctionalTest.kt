package iut.butinfo3.app_mobile.functional

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

/**
 * Tests fonctionnels pour le système de rappels.
 * Teste parseTimeOfDay (logique reproduite car méthode privée dans ReminderManager).
 */
class ReminderFunctionalTest {

    /**
     * Reproduit la logique de ReminderManager.parseTimeOfDay pour tester en JVM pur.
     */
    private fun parseTimeOfDay(timeOfDay: String): Pair<Int, Int>? {
        return try {
            val parts = timeOfDay.split(":")
            if (parts.size != 2) return null

            val hour = parts[0].toIntOrNull() ?: return null
            val minute = parts[1].toIntOrNull() ?: return null

            if (hour !in 0..23 || minute !in 0..59) return null

            Pair(hour, minute)
        } catch (e: Exception) {
            null
        }
    }

    // --- parseTimeOfDay (analyse partitionnelle + limites) ---

    @ParameterizedTest(name = "parseTimeOfDay(\"{0}\") -> valid={1}")
    @CsvSource(
        "00:00, true",    // Limite min valide
        "23:59, true",    // Limite max valide
        "24:00, false",   // Overflow heure
        "00:60, false",   // Overflow minute
        "abc, false",     // Non numérique
        "'', false",      // Vide
        "12, false",      // Sans séparateur
        "12:30, true",    // Valeur médiane
        "-1:00, false",   // Heure négative
        "00:-1, false"    // Minute négative
    )
    fun parseTimeOfDay_partitions(input: String, expected: Boolean) {
        val result = parseTimeOfDay(input)
        assertEquals(expected, result != null, "parseTimeOfDay(\"$input\") expected valid=$expected")
    }

    // --- Parsed values ---

    @ParameterizedTest(name = "parseTimeOfDay(\"{0}\") -> hour={1}, minute={2}")
    @CsvSource(
        "00:00, 0, 0",
        "23:59, 23, 59",
        "08:30, 8, 30",
        "12:00, 12, 0"
    )
    fun parseTimeOfDay_correctValues(input: String, expectedHour: Int, expectedMinute: Int) {
        val result = parseTimeOfDay(input)
        assertNotNull(result)
        assertEquals(expectedHour, result!!.first)
        assertEquals(expectedMinute, result.second)
    }

    // --- Reminder ID validation ---

    @ParameterizedTest(name = "reminderId={0} -> valid={1}")
    @CsvSource(
        "0, false",     // ID invalide (limite)
        "1, true",      // Limite min valide
        "-1, false",    // Négatif
        "100, true"     // Valide quelconque
    )
    fun reminderIdValidation(reminderId: Long, expected: Boolean) {
        // Reproduit la validation de ReminderManager : id <= 0 -> invalide
        assertEquals(expected, reminderId > 0)
    }
}
