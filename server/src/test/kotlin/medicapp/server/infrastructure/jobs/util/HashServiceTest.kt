package medicapp.server.infrastructure.jobs.util

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.io.FileNotFoundException
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue
import kotlin.test.assertFailsWith

class HashServiceTest {

    @TempDir
    lateinit var tempDir: Path

    private lateinit var file1: File
    private lateinit var file2: File
    private lateinit var file3: File

    @BeforeEach
    fun setup() {
        file1 = tempDir.resolve("file1.txt").toFile()
        file2 = tempDir.resolve("file2.txt").toFile()
        file3 = tempDir.resolve("file3.txt").toFile()
    }

    @AfterEach
    fun tearDown() {
        listOf(file1, file2, file3).forEach { it.delete() }
    }

    // ─────────────────────────────────────────────────────────────
    // Tests computeHash()
    // ─────────────────────────────────────────────────────────────

    @Test
    fun `computeHash retourne un hash SHA-256 valide`() {
        // Arrange
        file1.writeText("Hello, World!")

        // Act
        val hash = HashService.computeHash(file1)

        // Assert - SHA-256 produit un hash de 64 caractères hexadécimaux
        assertEquals(64, hash.length)
        assertTrue(hash.all { it in '0'..'9' || it in 'a'..'f' })
    }

    @Test
    fun `computeHash retourne le meme hash pour le meme contenu`() {
        // Arrange
        val content = "Contenu de test identique"
        file1.writeText(content)
        file2.writeText(content)

        // Act
        val hash1 = HashService.computeHash(file1)
        val hash2 = HashService.computeHash(file2)

        // Assert
        assertEquals(hash1, hash2)
    }

    @Test
    fun `computeHash retourne des hash differents pour des contenus differents`() {
        // Arrange
        file1.writeText("Contenu A")
        file2.writeText("Contenu B")

        // Act
        val hash1 = HashService.computeHash(file1)
        val hash2 = HashService.computeHash(file2)

        // Assert
        assertNotEquals(hash1, hash2)
    }

    @Test
    fun `computeHash gere les fichiers vides`() {
        // Arrange
        file1.writeText("")

        // Act
        val hash = HashService.computeHash(file1)

        // Assert - Hash SHA-256 d'un fichier vide (connu)
        assertEquals("e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855", hash)
    }

    @Test
    fun `computeHash gere les fichiers avec caracteres speciaux UTF-8`() {
        // Arrange
        file1.writeText("Médicament: Paracétamol 500mg - été 2024 \u00e9\u00e8\u00ea")

        // Act
        val hash = HashService.computeHash(file1)

        // Assert
        assertEquals(64, hash.length)
        assertTrue(hash.all { it in '0'..'9' || it in 'a'..'f' })
    }

    @Test
    fun `computeHash gere les fichiers binaires`() {
        // Arrange
        val binaryData = byteArrayOf(0x00, 0x01, 0x02, 0xFF.toByte(), 0xFE.toByte())
        file1.writeBytes(binaryData)

        // Act
        val hash = HashService.computeHash(file1)

        // Assert
        assertEquals(64, hash.length)
    }

    @Test
    fun `computeHash gere les gros fichiers`() {
        // Arrange - fichier de ~1MB
        val largeContent = "A".repeat(1024 * 1024)
        file1.writeText(largeContent)

        // Act
        val hash = HashService.computeHash(file1)

        // Assert
        assertEquals(64, hash.length)
    }

    @Test
    fun `computeHash lance FileNotFoundException pour fichier inexistant`() {
        // Arrange
        val nonExistentFile = tempDir.resolve("inexistant.txt").toFile()

        // Act & Assert
        assertFailsWith<FileNotFoundException> {
            HashService.computeHash(nonExistentFile)
        }
    }

    // ─────────────────────────────────────────────────────────────
    // Tests isSame()
    // ─────────────────────────────────────────────────────────────

    @Test
    fun `isSame retourne true pour fichier avec hash identique`() {
        // Arrange
        file1.writeText("Contenu de test")
        val referenceHash = HashService.computeHash(file1)

        // Act
        val result = HashService.isSame(file1, referenceHash)

        // Assert
        assertTrue(result)
    }

    @Test
    fun `isSame retourne false pour fichier avec hash different`() {
        // Arrange
        file1.writeText("Contenu de test")
        val wrongHash = "0000000000000000000000000000000000000000000000000000000000000000"

        // Act
        val result = HashService.isSame(file1, wrongHash)

        // Assert
        assertFalse(result)
    }

    @Test
    fun `isSame retourne false pour hash vide`() {
        // Arrange
        file1.writeText("Contenu de test")

        // Act
        val result = HashService.isSame(file1, "")

        // Assert
        assertFalse(result)
    }

    @Test
    fun `isSame detecte modification de contenu`() {
        // Arrange
        file1.writeText("Contenu original")
        val originalHash = HashService.computeHash(file1)
        file1.writeText("Contenu modifie")

        // Act
        val result = HashService.isSame(file1, originalHash)

        // Assert
        assertFalse(result)
    }

    @Test
    fun `isSame fonctionne avec fichier vide et hash vide attendu`() {
        // Arrange
        file1.writeText("")
        val emptyFileHash = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855"

        // Act
        val result = HashService.isSame(file1, emptyFileHash)

        // Assert
        assertTrue(result)
    }

    @Test
    fun `isSame lance FileNotFoundException pour fichier inexistant`() {
        // Arrange
        val nonExistentFile = tempDir.resolve("inexistant.txt").toFile()

        // Act & Assert
        assertFailsWith<FileNotFoundException> {
            HashService.isSame(nonExistentFile, "somehash")
        }
    }

    @Test
    fun `isSame est sensible a la casse du hash`() {
        // Arrange
        file1.writeText("Test")
        val hash = HashService.computeHash(file1)
        val upperCaseHash = hash.uppercase()

        // Act
        val result = HashService.isSame(file1, upperCaseHash)

        // Assert - le hash est en minuscules, donc uppercase devrait etre different
        assertFalse(result)
    }
}
