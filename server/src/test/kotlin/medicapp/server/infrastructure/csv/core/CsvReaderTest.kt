package medicapp.server.infrastructure.csv.core

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.io.FileNotFoundException
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertFailsWith

class CsvReaderTest {

    @TempDir
    lateinit var tempDir: Path

    private lateinit var csvFile: File

    @BeforeEach
    fun setup() {
        csvFile = tempDir.resolve("test.csv").toFile()
    }

    @AfterEach
    fun tearDown() {
        csvFile.delete()
    }

    // ─────────────────────────────────────────────────────────────
    // Tests cas nominaux
    // ─────────────────────────────────────────────────────────────

    @Test
    fun `read fichier CSV valide retourne liste de Map`() {
        // Arrange
        csvFile.writeText("nom,prenom,age\nDupont,Jean,30\nMartin,Marie,25")

        // Act
        val result = CsvReader.read(csvFile)

        // Assert
        assertEquals(2, result.size)
        assertEquals("Dupont", result[0]["nom"])
        assertEquals("Jean", result[0]["prenom"])
        assertEquals("30", result[0]["age"])
        assertEquals("Martin", result[1]["nom"])
        assertEquals("Marie", result[1]["prenom"])
        assertEquals("25", result[1]["age"])
    }

    @Test
    fun `read fichier avec une seule ligne de donnees`() {
        // Arrange
        csvFile.writeText("cis,libelle\n60000001,DOLIPRANE")

        // Act
        val result = CsvReader.read(csvFile)

        // Assert
        assertEquals(1, result.size)
        assertEquals("60000001", result[0]["cis"])
        assertEquals("DOLIPRANE", result[0]["libelle"])
    }

    @Test
    fun `read fichier avec plusieurs colonnes`() {
        // Arrange
        csvFile.writeText("col1,col2,col3,col4,col5\na,b,c,d,e")

        // Act
        val result = CsvReader.read(csvFile)

        // Assert
        assertEquals(1, result.size)
        assertEquals(5, result[0].keys.size)
        assertEquals("a", result[0]["col1"])
        assertEquals("e", result[0]["col5"])
    }

    // ─────────────────────────────────────────────────────────────
    // Tests fichiers vides / limites
    // ─────────────────────────────────────────────────────────────

    @Test
    fun `read fichier avec seulement entete retourne liste vide`() {
        // Arrange
        csvFile.writeText("nom,prenom,age")

        // Act
        val result = CsvReader.read(csvFile)

        // Assert
        assertEquals(0, result.size)
    }

    @Test
    fun `read fichier vide retourne liste vide`() {
        // Arrange
        csvFile.writeText("")

        // Act
        val result = CsvReader.read(csvFile)

        // Assert
        assertTrue(result.isEmpty())
    }

    // ─────────────────────────────────────────────────────────────
    // Tests encodage UTF-8
    // ─────────────────────────────────────────────────────────────

    @Test
    fun `read fichier avec caracteres accentues UTF-8`() {
        // Arrange
        csvFile.writeText("medicament,fabricant\nParacetamol,Sanofi-Aventis\nIbuprofene,Mylan Generiques", Charsets.UTF_8)

        // Act
        val result = CsvReader.read(csvFile)

        // Assert
        assertEquals(2, result.size)
        assertEquals("Paracetamol", result[0]["medicament"])
        assertEquals("Ibuprofene", result[1]["medicament"])
    }

    @Test
    fun `read fichier avec caracteres speciaux`() {
        // Arrange
        csvFile.writeText("nom,description\nTest,Valeur avec eau et c", Charsets.UTF_8)

        // Act
        val result = CsvReader.read(csvFile)

        // Assert
        assertEquals("Valeur avec eau et c", result[0]["description"])
    }

    // ─────────────────────────────────────────────────────────────
    // Tests valeurs avec guillemets et virgules
    // ─────────────────────────────────────────────────────────────

    @Test
    fun `read fichier avec valeurs contenant des virgules entre guillemets`() {
        // Arrange
        csvFile.writeText("nom,adresse\nPharmacie,\"12 rue de Paris, 75001\"")

        // Act
        val result = CsvReader.read(csvFile)

        // Assert
        assertEquals(1, result.size)
        assertEquals("12 rue de Paris, 75001", result[0]["adresse"])
    }

    @Test
    fun `read fichier avec valeurs contenant des guillemets echappes`() {
        // Arrange
        csvFile.writeText("nom,citation\nTest,\"Il a dit \"\"bonjour\"\"\"")

        // Act
        val result = CsvReader.read(csvFile)

        // Assert
        assertEquals(1, result.size)
        assertEquals("Il a dit \"bonjour\"", result[0]["citation"])
    }

    @Test
    fun `read fichier avec valeurs contenant des sauts de ligne entre guillemets`() {
        // Arrange
        val content = "nom,description\nTest,\"Ligne 1\nLigne 2\""
        csvFile.writeText(content)

        // Act
        val result = CsvReader.read(csvFile)

        // Assert
        assertEquals(1, result.size)
        assertEquals("Ligne 1\nLigne 2", result[0]["description"])
    }

    // ─────────────────────────────────────────────────────────────
    // Tests valeurs vides
    // ─────────────────────────────────────────────────────────────

    @Test
    fun `read fichier avec valeurs vides`() {
        // Arrange
        csvFile.writeText("nom,prenom,age\nDupont,,30\n,Marie,")

        // Act
        val result = CsvReader.read(csvFile)

        // Assert
        assertEquals(2, result.size)
        assertEquals("Dupont", result[0]["nom"])
        assertEquals("", result[0]["prenom"])
        assertEquals("30", result[0]["age"])
        assertEquals("", result[1]["nom"])
        assertEquals("Marie", result[1]["prenom"])
        assertEquals("", result[1]["age"])
    }

    // ─────────────────────────────────────────────────────────────
    // Tests erreurs
    // ─────────────────────────────────────────────────────────────

    @Test
    fun `read fichier inexistant lance FileNotFoundException`() {
        // Arrange
        val nonExistentFile = tempDir.resolve("inexistant.csv").toFile()

        // Act & Assert
        assertFailsWith<FileNotFoundException> {
            CsvReader.read(nonExistentFile)
        }
    }

    // ─────────────────────────────────────────────────────────────
    // Tests cas metier specifiques
    // ─────────────────────────────────────────────────────────────

    @Test
    fun `read fichier CSV format RUIM avec colonnes typiques`() {
        // Arrange - format similaire aux donnees RUIM
        csvFile.writeText("CIS,Denomination,FormePharma,VoiesAdmin\n60000001,DOLIPRANE 1000 mg comprime,comprime,orale\n60000002,ASPIRINE 500 mg,comprime effervescent,orale")

        // Act
        val result = CsvReader.read(csvFile)

        // Assert
        assertEquals(2, result.size)
        assertEquals("60000001", result[0]["CIS"])
        assertEquals("DOLIPRANE 1000 mg comprime", result[0]["Denomination"])
    }

    @Test
    fun `read fichier CSV format SMS avec colonnes typiques`() {
        // Arrange - format similaire aux donnees SMS
        val header = "#SMS_ID,Substance_Domain,Language,Substance_Name"
        val row1 = "SMS001,Human use,French,Paracetamol"
        val row2 = "SMS002,Human use,English,Ibuprofen"
        csvFile.writeText("$header\n$row1\n$row2")

        // Act
        val result = CsvReader.read(csvFile)

        // Assert
        assertEquals(2, result.size)
        assertEquals("SMS001", result[0]["#SMS_ID"])
        assertEquals("Human use", result[0]["Substance_Domain"])
        assertEquals("French", result[0]["Language"])
    }

    @Test
    fun `read fichier avec beaucoup de lignes`() {
        // Arrange - 1000 lignes
        val header = "id,value"
        val lines = (1..1000).map { "$it,value_$it" }
        csvFile.writeText((listOf(header) + lines).joinToString("\n"))

        // Act
        val result = CsvReader.read(csvFile)

        // Assert
        assertEquals(1000, result.size)
        assertEquals("1", result[0]["id"])
        assertEquals("value_1", result[0]["value"])
        assertEquals("1000", result[999]["id"])
        assertEquals("value_1000", result[999]["value"])
    }

    @Test
    fun `read fichier avec espaces autour des valeurs`() {
        // Arrange
        csvFile.writeText("nom,prenom\n Dupont , Jean ")

        // Act
        val result = CsvReader.read(csvFile)

        // Assert
        assertEquals(1, result.size)
        // Les espaces sont preserves par defaut dans kotlin-csv
        assertEquals(" Dupont ", result[0]["nom"])
        assertEquals(" Jean ", result[0]["prenom"])
    }
}
