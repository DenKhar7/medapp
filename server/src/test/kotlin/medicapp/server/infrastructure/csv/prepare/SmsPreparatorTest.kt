package medicapp.server.infrastructure.csv.prepare

import org.junit.jupiter.api.BeforeEach
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SmsPreparatorTest {

    private lateinit var preparator: SmsPreparator

    @BeforeEach
    fun setup() {
        preparator = SmsPreparator()
    }

    // ─────────────────────────────────────────────────────────────
    // Tests filtrage par Substance_Domain
    // ─────────────────────────────────────────────────────────────

    @Test
    fun `prepare filtre uniquement les entrees Human use`() {
        // Arrange
        val input = listOf(
            createSmsRow("SMS001", "Human use", "French", "Paracetamol"),
            createSmsRow("SMS002", "Veterinary use", "French", "Vetmed"),
            createSmsRow("SMS003", "Human use", "English", "Ibuprofen")
        )

        // Act
        val result = preparator.prepare(input)

        // Assert
        assertEquals(2, result.data.size)
        assertTrue(result.data.all { it["Substance_Domain"] == "Human use" })
    }

    @Test
    fun `prepare ignore les entrees sans Substance_Domain Human use`() {
        // Arrange
        val input = listOf(
            createSmsRow("SMS001", "Veterinary use", "French", "VetMed"),
            createSmsRow("SMS002", "Other", "French", "Other"),
            createSmsRow("SMS003", "", "French", "Empty")
        )

        // Act
        val result = preparator.prepare(input)

        // Assert
        assertEquals(0, result.data.size)
    }

    // ─────────────────────────────────────────────────────────────
    // Tests priorite de langue (FR > EN)
    // ─────────────────────────────────────────────────────────────

    @Test
    fun `prepare selectionne French en priorite sur English`() {
        // Arrange
        val input = listOf(
            createSmsRow("SMS001", "Human use", "English", "Ibuprofen EN"),
            createSmsRow("SMS001", "Human use", "French", "Ibuprofene FR")
        )

        // Act
        val result = preparator.prepare(input)

        // Assert
        assertEquals(1, result.data.size)
        assertEquals("French", result.data[0]["Language"])
        assertEquals("Ibuprofene FR", result.data[0]["Substance_Name"])
    }

    @Test
    fun `prepare selectionne English si French absent`() {
        // Arrange
        val input = listOf(
            createSmsRow("SMS001", "Human use", "English", "Ibuprofen EN"),
            createSmsRow("SMS001", "Human use", "German", "Ibuprofen DE")
        )

        // Act
        val result = preparator.prepare(input)

        // Assert
        assertEquals(1, result.data.size)
        assertEquals("English", result.data[0]["Language"])
    }

    @Test
    fun `prepare conserve French meme si English presente apres`() {
        // Arrange
        val input = listOf(
            createSmsRow("SMS001", "Human use", "French", "Paracetamol FR"),
            createSmsRow("SMS001", "Human use", "English", "Paracetamol EN")
        )

        // Act
        val result = preparator.prepare(input)

        // Assert
        assertEquals(1, result.data.size)
        assertEquals("French", result.data[0]["Language"])
    }

    // ─────────────────────────────────────────────────────────────
    // Tests dropped count
    // ─────────────────────────────────────────────────────────────

    @Test
    fun `prepare compte correctement les entrees sans langue FR ou EN`() {
        // Arrange
        val input = listOf(
            createSmsRow("SMS001", "Human use", "French", "Paracetamol"),
            createSmsRow("SMS002", "Human use", "German", "Aspirin"),
            createSmsRow("SMS003", "Human use", "Spanish", "Ibuprofeno"),
            createSmsRow("SMS004", "Human use", "Italian", "Amoxicillina")
        )

        // Act
        val result = preparator.prepare(input)

        // Assert
        assertEquals(1, result.data.size)  // SMS001 avec French
        assertEquals(3, result.dropped)     // SMS002, SMS003, SMS004 sans FR/EN
    }

    @Test
    fun `prepare retourne dropped 0 quand toutes les entrees ont FR ou EN`() {
        // Arrange
        val input = listOf(
            createSmsRow("SMS001", "Human use", "French", "Paracetamol"),
            createSmsRow("SMS002", "Human use", "English", "Ibuprofen")
        )

        // Act
        val result = preparator.prepare(input)

        // Assert
        assertEquals(2, result.data.size)
        assertEquals(0, result.dropped)
    }

    // ─────────────────────────────────────────────────────────────
    // Tests groupage par SMS_ID
    // ─────────────────────────────────────────────────────────────

    @Test
    fun `prepare groupe par SMS_ID et conserve une seule entree par groupe`() {
        // Arrange
        val input = listOf(
            createSmsRow("SMS001", "Human use", "French", "Paracetamol FR"),
            createSmsRow("SMS001", "Human use", "English", "Paracetamol EN"),
            createSmsRow("SMS002", "Human use", "French", "Ibuprofen FR"),
            createSmsRow("SMS002", "Human use", "English", "Ibuprofen EN")
        )

        // Act
        val result = preparator.prepare(input)

        // Assert
        assertEquals(2, result.data.size)
        assertEquals(setOf("SMS001", "SMS002"), result.data.map { it["#SMS_ID"] }.toSet())
    }

    @Test
    fun `prepare gere les SMS_ID uniques correctement`() {
        // Arrange
        val input = listOf(
            createSmsRow("SMS001", "Human use", "French", "Substance1"),
            createSmsRow("SMS002", "Human use", "English", "Substance2"),
            createSmsRow("SMS003", "Human use", "French", "Substance3")
        )

        // Act
        val result = preparator.prepare(input)

        // Assert
        assertEquals(3, result.data.size)
        assertEquals(0, result.dropped)
    }

    // ─────────────────────────────────────────────────────────────
    // Tests cas limites
    // ─────────────────────────────────────────────────────────────

    @Test
    fun `prepare avec liste vide retourne resultat vide`() {
        // Arrange
        val input = emptyList<Map<String, String>>()

        // Act
        val result = preparator.prepare(input)

        // Assert
        assertEquals(0, result.data.size)
        assertEquals(0, result.dropped)
    }

    @Test
    fun `prepare avec toutes entrees filtrees par domain retourne liste vide`() {
        // Arrange
        val input = listOf(
            createSmsRow("SMS001", "Veterinary use", "French", "VetMed1"),
            createSmsRow("SMS002", "Veterinary use", "English", "VetMed2")
        )

        // Act
        val result = preparator.prepare(input)

        // Assert
        assertEquals(0, result.data.size)
        assertEquals(0, result.dropped)  // dropped ne compte que les Human use sans FR/EN
    }

    @Test
    fun `prepare avec toutes entrees filtrees par langue`() {
        // Arrange
        val input = listOf(
            createSmsRow("SMS001", "Human use", "German", "Aspirin"),
            createSmsRow("SMS002", "Human use", "Spanish", "Aspirina"),
            createSmsRow("SMS003", "Human use", "Italian", "Aspirina")
        )

        // Act
        val result = preparator.prepare(input)

        // Assert
        assertEquals(0, result.data.size)
        assertEquals(3, result.dropped)
    }

    // ─────────────────────────────────────────────────────────────
    // Tests warnings
    // ─────────────────────────────────────────────────────────────

    @Test
    fun `prepare genere un warning avec le nombre d entrees ignorees`() {
        // Arrange
        val input = listOf(
            createSmsRow("SMS001", "Human use", "French", "Valid"),
            createSmsRow("SMS002", "Human use", "German", "Ignored1"),
            createSmsRow("SMS003", "Human use", "Spanish", "Ignored2")
        )

        // Act
        val result = preparator.prepare(input)

        // Assert
        assertEquals(1, result.warnings.size)
        assertTrue(result.warnings[0].contains("2"))
        assertTrue(result.warnings[0].contains("SMS"))
    }

    // ─────────────────────────────────────────────────────────────
    // Tests cas complexes
    // ─────────────────────────────────────────────────────────────

    @Test
    fun `prepare scenario complet avec melange de cas`() {
        // Arrange
        val input = listOf(
            // SMS001: FR disponible -> garder FR
            createSmsRow("SMS001", "Human use", "French", "FR1"),
            createSmsRow("SMS001", "Human use", "English", "EN1"),
            // SMS002: seulement EN -> garder EN
            createSmsRow("SMS002", "Human use", "English", "EN2"),
            createSmsRow("SMS002", "Human use", "German", "DE2"),
            // SMS003: aucune langue FR/EN -> dropped
            createSmsRow("SMS003", "Human use", "German", "DE3"),
            // SMS004: filtre par domain
            createSmsRow("SMS004", "Veterinary use", "French", "VET"),
            // SMS005: FR seul
            createSmsRow("SMS005", "Human use", "French", "FR5")
        )

        // Act
        val result = preparator.prepare(input)

        // Assert
        assertEquals(3, result.data.size)  // SMS001 (FR), SMS002 (EN), SMS005 (FR)
        assertEquals(1, result.dropped)     // SMS003 (pas de FR/EN)

        val sms001 = result.data.find { it["#SMS_ID"] == "SMS001" }
        assertEquals("French", sms001!!["Language"])

        val sms002 = result.data.find { it["#SMS_ID"] == "SMS002" }
        assertEquals("English", sms002!!["Language"])
    }

    @Test
    fun `prepare preserve toutes les colonnes de la ligne selectionnee`() {
        // Arrange
        val input = listOf(
            mapOf(
                "#SMS_ID" to "SMS001",
                "Substance_Domain" to "Human use",
                "Language" to "French",
                "Substance_Name" to "Paracetamol",
                "Extra_Column" to "extra_value",
                "Another_Column" to "another_value"
            )
        )

        // Act
        val result = preparator.prepare(input)

        // Assert
        assertEquals(1, result.data.size)
        assertEquals("extra_value", result.data[0]["Extra_Column"])
        assertEquals("another_value", result.data[0]["Another_Column"])
    }

    // ─────────────────────────────────────────────────────────────
    // Methode helper
    // ─────────────────────────────────────────────────────────────

    private fun createSmsRow(
        smsId: String,
        substanceDomain: String,
        language: String,
        substanceName: String
    ): Map<String, String> = mapOf(
        "#SMS_ID" to smsId,
        "Substance_Domain" to substanceDomain,
        "Language" to language,
        "Substance_Name" to substanceName
    )
}
