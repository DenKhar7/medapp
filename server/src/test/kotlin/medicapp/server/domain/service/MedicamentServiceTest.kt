package medicapp.server.domain.service

import io.mockk.*
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import medicapp.server.domain.businessclass.medicament.*
import medicapp.server.domain.repository.MedicamentRepository
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.test.assertFalse

class MedicamentServiceTest {

    private lateinit var repo: MedicamentRepository
    private lateinit var service: MedicamentService

    @BeforeEach
    fun setup() {
        repo = mockk()
        service = MedicamentService(repo)
    }

    @AfterEach
    fun tearDown() {
        clearAllMocks()
    }

    // ─────────────────────────────────────────────────────────────
    // Tests Spécialités (CIS)
    // ─────────────────────────────────────────────────────────────

    @Test
    fun `getSpeResumeByCis delegue au repository`() = runTest {
        // Arrange
        val expected = MedicamentSpeResume(
            cis = 60000001,
            nom = "DOLIPRANE 1000 mg, comprimé",
            nomOrganisation = "SANOFI AVENTIS FRANCE",
            codeAtc = "N02BE01",
            libelleAtc = "paracétamol",
            voies = listOf("orale")
        )
        coEvery { repo.getSpeResumeByCis(60000001) } returns expected

        // Act
        val result = service.getSpeResumeByCis(60000001)

        // Assert
        assertEquals(expected, result)
        coVerify(exactly = 1) { repo.getSpeResumeByCis(60000001) }
    }

    @Test
    fun `getSpeResumeByCis retourne null si non trouve`() = runTest {
        // Arrange
        coEvery { repo.getSpeResumeByCis(99999999) } returns null

        // Act
        val result = service.getSpeResumeByCis(99999999)

        // Assert
        assertNull(result)
        coVerify(exactly = 1) { repo.getSpeResumeByCis(99999999) }
    }

    @Test
    fun `searchSpeResumeByLibelle retourne liste vide si libelle vide`() = runTest {
        // Arrange - pas de setup nécessaire pour input blank

        // Act
        val result = service.searchSpeResumeByLibelle("")

        // Assert
        assertEquals(emptyList(), result)
        coVerify(exactly = 0) { repo.searchSpeResumeByLibelle(any(), any()) }
    }

    @Test
    fun `searchSpeResumeByLibelle retourne liste vide si libelle whitespace`() = runTest {
        // Arrange

        // Act
        val result = service.searchSpeResumeByLibelle("   ")

        // Assert
        assertEquals(emptyList(), result)
        coVerify(exactly = 0) { repo.searchSpeResumeByLibelle(any(), any()) }
    }

    @Test
    fun `searchSpeResumeByLibelle delegue au repo avec limit par defaut`() = runTest {
        // Arrange
        val expected = listOf(
            MedicamentSpeResume(60000001, "DOLIPRANE", "SANOFI", "N02BE01", "paracétamol", listOf("orale"))
        )
        coEvery { repo.searchSpeResumeByLibelle("doliprane", 10) } returns expected

        // Act
        val result = service.searchSpeResumeByLibelle("doliprane")

        // Assert
        assertEquals(expected, result)
        coVerify(exactly = 1) { repo.searchSpeResumeByLibelle("doliprane", 10) }
    }

    @Test
    fun `searchSpeResumeByLibelle delegue au repo avec limit explicite`() = runTest {
        // Arrange
        val expected = listOf(
            MedicamentSpeResume(60000001, "DOLIPRANE", "SANOFI", "N02BE01", "paracétamol", listOf("orale"))
        )
        coEvery { repo.searchSpeResumeByLibelle("doliprane", 25) } returns expected

        // Act
        val result = service.searchSpeResumeByLibelle("doliprane", 25)

        // Assert
        assertEquals(expected, result)
        coVerify(exactly = 1) { repo.searchSpeResumeByLibelle("doliprane", 25) }
    }

    @Test
    fun `getLibelleCompletion retourne liste vide si libelleExtract blank`() = runTest {
        // Arrange

        // Act
        val result = service.getLibelleCompletion("")

        // Assert
        assertEquals(emptyList(), result)
        coVerify(exactly = 0) { repo.getLibelleCompletion(any(), any()) }
    }

    @Test
    fun `getLibelleCompletion retourne liste vide si libelleExtract whitespace`() = runTest {
        // Arrange

        // Act
        val result = service.getLibelleCompletion("   ")

        // Assert
        assertEquals(emptyList(), result)
        coVerify(exactly = 0) { repo.getLibelleCompletion(any(), any()) }
    }

    @Test
    fun `getLibelleCompletion delegue au repo avec input valide`() = runTest {
        // Arrange
        val expected = listOf(
            MedicamentLibelleCompletion(60000001, "DOLIPRANE 1000 mg")
        )
        coEvery { repo.getLibelleCompletion("doli", 10) } returns expected

        // Act
        val result = service.getLibelleCompletion("doli")

        // Assert
        assertEquals(expected, result)
        coVerify(exactly = 1) { repo.getLibelleCompletion("doli", 10) }
    }

    @Test
    fun `isSpeMarketed delegue au repository et retourne true`() = runTest {
        // Arrange
        coEvery { repo.isSpeMarketed(60000001) } returns true

        // Act
        val result = service.isSpeMarketed(60000001)

        // Assert
        assertTrue(result)
        coVerify(exactly = 1) { repo.isSpeMarketed(60000001) }
    }

    @Test
    fun `isSpeMarketed delegue au repository et retourne false`() = runTest {
        // Arrange
        coEvery { repo.isSpeMarketed(99999999) } returns false

        // Act
        val result = service.isSpeMarketed(99999999)

        // Assert
        assertFalse(result)
        coVerify(exactly = 1) { repo.isSpeMarketed(99999999) }
    }

    // ─────────────────────────────────────────────────────────────
    // Tests Présentations (CIP)
    // ─────────────────────────────────────────────────────────────

    @Test
    fun `getAllPresDetailByCis delegue au repository`() = runTest {
        // Arrange
        val expected = listOf(
            MedicamentPresDetail(
                cip13 = "3400930000001",
                label = "DOLIPRANE 1000 mg, comprimé - boîte de 8",
                nomSpecialite = "DOLIPRANE 1000 mg, comprimé",
                cis = 60000001,
                nomOrganisation = "SANOFI",
                codeAtc = "N02BE01",
                libelleAtc = "paracétamol",
                voie = listOf("orale"),
                dosesParBoite = 8,
                quantiteConditionnement = 1.0,
                uniteConditionnement = "boîte",
                typeDispositif = null
            )
        )
        coEvery { repo.getAllPresDetailByCis(60000001) } returns expected

        // Act
        val result = service.getAllPresDetailByCis(60000001)

        // Assert
        assertEquals(expected, result)
        coVerify(exactly = 1) { repo.getAllPresDetailByCis(60000001) }
    }

    @Test
    fun `getAllPresDetailByCis retourne liste vide si aucune`() = runTest {
        // Arrange
        coEvery { repo.getAllPresDetailByCis(99999999) } returns emptyList()

        // Act
        val result = service.getAllPresDetailByCis(99999999)

        // Assert
        assertEquals(emptyList(), result)
        coVerify(exactly = 1) { repo.getAllPresDetailByCis(99999999) }
    }

    @Test
    fun `getPresDetailByCip delegue au repository`() = runTest {
        // Arrange
        val expected = MedicamentPresDetail(
            cip13 = "3400930000001",
            label = "DOLIPRANE 1000 mg, comprimé - boîte de 8",
            nomSpecialite = "DOLIPRANE 1000 mg, comprimé",
            cis = 60000001,
            nomOrganisation = "SANOFI",
            codeAtc = "N02BE01",
            libelleAtc = "paracétamol",
            voie = listOf("orale"),
            dosesParBoite = 8,
            quantiteConditionnement = 1.0,
            uniteConditionnement = "boîte",
            typeDispositif = null
        )
        coEvery { repo.getPresDetailByCip("3400930000001") } returns expected

        // Act
        val result = service.getPresDetailByCip("3400930000001")

        // Assert
        assertEquals(expected, result)
        coVerify(exactly = 1) { repo.getPresDetailByCip("3400930000001") }
    }

    @Test
    fun `getPresDetailByCip retourne null si non trouve`() = runTest {
        // Arrange
        coEvery { repo.getPresDetailByCip("0000000000000") } returns null

        // Act
        val result = service.getPresDetailByCip("0000000000000")

        // Assert
        assertNull(result)
        coVerify(exactly = 1) { repo.getPresDetailByCip("0000000000000") }
    }

    @Test
    fun `searchPresDetailByLibelle retourne liste vide si libelle blank`() = runTest {
        // Arrange

        // Act
        val result = service.searchPresDetailByLibelle("")

        // Assert
        assertEquals(emptyList(), result)
        coVerify(exactly = 0) { repo.searchPresDetailByLibelle(any(), any()) }
    }

    @Test
    fun `searchPresDetailByLibelle retourne liste vide si libelle whitespace`() = runTest {
        // Arrange

        // Act
        val result = service.searchPresDetailByLibelle("   ")

        // Assert
        assertEquals(emptyList(), result)
        coVerify(exactly = 0) { repo.searchPresDetailByLibelle(any(), any()) }
    }

    @Test
    fun `searchPresDetailByLibelle delegue au repo avec input valide`() = runTest {
        // Arrange
        val expected = listOf(
            MedicamentPresDetail(
                cip13 = "3400930000001",
                label = "DOLIPRANE 1000 mg",
                nomSpecialite = "DOLIPRANE",
                cis = 60000001,
                nomOrganisation = "SANOFI",
                codeAtc = "N02BE01",
                libelleAtc = "paracétamol",
                voie = listOf("orale"),
                dosesParBoite = 8,
                quantiteConditionnement = null,
                uniteConditionnement = null,
                typeDispositif = null
            )
        )
        coEvery { repo.searchPresDetailByLibelle("doliprane", 10) } returns expected

        // Act
        val result = service.searchPresDetailByLibelle("doliprane")

        // Assert
        assertEquals(expected, result)
        coVerify(exactly = 1) { repo.searchPresDetailByLibelle("doliprane", 10) }
    }

    @Test
    fun `isPresMarketed delegue au repository et retourne true`() = runTest {
        // Arrange
        coEvery { repo.isPresMarketed("3400930000001") } returns true

        // Act
        val result = service.isPresMarketed("3400930000001")

        // Assert
        assertTrue(result)
        coVerify(exactly = 1) { repo.isPresMarketed("3400930000001") }
    }

    @Test
    fun `isPresMarketed delegue au repository et retourne false`() = runTest {
        // Arrange
        coEvery { repo.isPresMarketed("0000000000000") } returns false

        // Act
        val result = service.isPresMarketed("0000000000000")

        // Assert
        assertFalse(result)
        coVerify(exactly = 1) { repo.isPresMarketed("0000000000000") }
    }

    // ─────────────────────────────────────────────────────────────
    // Tests Substances
    // ─────────────────────────────────────────────────────────────

    @Test
    fun `getSubstancesResumeByCis delegue au repository`() = runTest {
        // Arrange
        val expected = listOf(
            MedicamentSubstanceResume(
                codeSubstance = "2065",
                libelle = "PARACÉTAMOL",
                relationSubstance = "principe actif",
                expressionQuantite = "1000 mg",
                substanceActive = 1,
                fractionTherapeutique = null
            )
        )
        coEvery { repo.getSubstancesResumeByCis(60000001) } returns expected

        // Act
        val result = service.getSubstancesResumeByCis(60000001)

        // Assert
        assertEquals(expected, result)
        coVerify(exactly = 1) { repo.getSubstancesResumeByCis(60000001) }
    }

    @Test
    fun `getSubstancesResumeByCis retourne liste vide si aucune`() = runTest {
        // Arrange
        coEvery { repo.getSubstancesResumeByCis(99999999) } returns emptyList()

        // Act
        val result = service.getSubstancesResumeByCis(99999999)

        // Assert
        assertEquals(emptyList(), result)
        coVerify(exactly = 1) { repo.getSubstancesResumeByCis(99999999) }
    }

    @Test
    fun `getSubstanceResumeByCisAndCode delegue au repository`() = runTest {
        // Arrange
        val expected = MedicamentSubstanceResume(
            codeSubstance = "2065",
            libelle = "PARACÉTAMOL",
            relationSubstance = "principe actif",
            expressionQuantite = "1000 mg",
            substanceActive = 1,
            fractionTherapeutique = null
        )
        coEvery { repo.getSubstanceResumeByCisAndCode(60000001, "2065") } returns expected

        // Act
        val result = service.getSubstanceResumeByCisAndCode(60000001, "2065")

        // Assert
        assertEquals(expected, result)
        coVerify(exactly = 1) { repo.getSubstanceResumeByCisAndCode(60000001, "2065") }
    }

    @Test
    fun `getSubstanceResumeByCisAndCode retourne null si non trouve`() = runTest {
        // Arrange
        coEvery { repo.getSubstanceResumeByCisAndCode(60000001, "9999") } returns null

        // Act
        val result = service.getSubstanceResumeByCisAndCode(60000001, "9999")

        // Assert
        assertNull(result)
        coVerify(exactly = 1) { repo.getSubstanceResumeByCisAndCode(60000001, "9999") }
    }

    @Test
    fun `getSubstancesDetailByCis delegue au repository`() = runTest {
        // Arrange
        val expected = listOf(
            MedicamentSubstanceDetail(
                nomSubstance = "PARACÉTAMOL",
                typeSubstance = "principe actif",
                formuleMoleculaire = "C8H9NO2",
                poidMoleculaire = 151.16f
            )
        )
        coEvery { repo.getSubstancesDetailByCis(60000001) } returns expected

        // Act
        val result = service.getSubstancesDetailByCis(60000001)

        // Assert
        assertEquals(expected, result)
        coVerify(exactly = 1) { repo.getSubstancesDetailByCis(60000001) }
    }

    @Test
    fun `getSubstancesDetailByCis retourne liste vide si aucune`() = runTest {
        // Arrange
        coEvery { repo.getSubstancesDetailByCis(99999999) } returns emptyList()

        // Act
        val result = service.getSubstancesDetailByCis(99999999)

        // Assert
        assertEquals(emptyList(), result)
        coVerify(exactly = 1) { repo.getSubstancesDetailByCis(99999999) }
    }

    @Test
    fun `getSubstanceDetailByCisAndCode delegue au repository`() = runTest {
        // Arrange
        val expected = MedicamentSubstanceDetail(
            nomSubstance = "PARACÉTAMOL",
            typeSubstance = "principe actif",
            formuleMoleculaire = "C8H9NO2",
            poidMoleculaire = 151.16f
        )
        coEvery { repo.getSubstanceDetailByCisAndCode(60000001, "2065") } returns expected

        // Act
        val result = service.getSubstanceDetailByCisAndCode(60000001, "2065")

        // Assert
        assertEquals(expected, result)
        coVerify(exactly = 1) { repo.getSubstanceDetailByCisAndCode(60000001, "2065") }
    }

    @Test
    fun `getSubstanceDetailByCisAndCode retourne null si non trouve`() = runTest {
        // Arrange
        coEvery { repo.getSubstanceDetailByCisAndCode(60000001, "9999") } returns null

        // Act
        val result = service.getSubstanceDetailByCisAndCode(60000001, "9999")

        // Assert
        assertNull(result)
        coVerify(exactly = 1) { repo.getSubstanceDetailByCisAndCode(60000001, "9999") }
    }

    // ─────────────────────────────────────────────────────────────
    // Tests Événements réglementaires
    // ─────────────────────────────────────────────────────────────

    @Test
    fun `getLatestEventByCip delegue au repository`() = runTest {
        // Arrange
        val expected = MedicamentPresStatus(
            cip13 = "3400930000001",
            dateEffet = LocalDate(2024, 1, 15),
            typeEvenement = "Commercialisation"
        )
        coEvery { repo.getLatestEventByCip("3400930000001") } returns expected

        // Act
        val result = service.getLatestEventByCip("3400930000001")

        // Assert
        assertEquals(expected, result)
        coVerify(exactly = 1) { repo.getLatestEventByCip("3400930000001") }
    }

    @Test
    fun `getLatestEventByCip retourne null si aucun evenement`() = runTest {
        // Arrange
        coEvery { repo.getLatestEventByCip("0000000000000") } returns null

        // Act
        val result = service.getLatestEventByCip("0000000000000")

        // Assert
        assertNull(result)
        coVerify(exactly = 1) { repo.getLatestEventByCip("0000000000000") }
    }

    @Test
    fun `getAllEventsByCip delegue au repository`() = runTest {
        // Arrange
        val expected = listOf(
            MedicamentPresStatus("3400930000001", LocalDate(2024, 1, 15), "Commercialisation"),
            MedicamentPresStatus("3400930000001", LocalDate(2020, 6, 1), "Autorisation")
        )
        coEvery { repo.getAllEventsByCip("3400930000001") } returns expected

        // Act
        val result = service.getAllEventsByCip("3400930000001")

        // Assert
        assertEquals(expected, result)
        coVerify(exactly = 1) { repo.getAllEventsByCip("3400930000001") }
    }

    @Test
    fun `getAllEventsByCip retourne liste vide si aucun`() = runTest {
        // Arrange
        coEvery { repo.getAllEventsByCip("0000000000000") } returns emptyList()

        // Act
        val result = service.getAllEventsByCip("0000000000000")

        // Assert
        assertEquals(emptyList(), result)
        coVerify(exactly = 1) { repo.getAllEventsByCip("0000000000000") }
    }

    @Test
    fun `getLatestEventByCis delegue au repository`() = runTest {
        // Arrange
        val expected = MedicamentSpeStatus(
            cis = 60000001,
            dateEffet = LocalDate(2024, 1, 15),
            typeEvenement = "Commercialisation"
        )
        coEvery { repo.getLatestEventByCis(60000001) } returns expected

        // Act
        val result = service.getLatestEventByCis(60000001)

        // Assert
        assertEquals(expected, result)
        coVerify(exactly = 1) { repo.getLatestEventByCis(60000001) }
    }

    @Test
    fun `getLatestEventByCis retourne null si aucun evenement`() = runTest {
        // Arrange
        coEvery { repo.getLatestEventByCis(99999999) } returns null

        // Act
        val result = service.getLatestEventByCis(99999999)

        // Assert
        assertNull(result)
        coVerify(exactly = 1) { repo.getLatestEventByCis(99999999) }
    }

    @Test
    fun `getAllEventsByCis delegue au repository`() = runTest {
        // Arrange
        val expected = listOf(
            MedicamentSpeStatus(60000001, LocalDate(2024, 1, 15), "Commercialisation"),
            MedicamentSpeStatus(60000001, LocalDate(2020, 6, 1), "Autorisation")
        )
        coEvery { repo.getAllEventsByCis(60000001) } returns expected

        // Act
        val result = service.getAllEventsByCis(60000001)

        // Assert
        assertEquals(expected, result)
        coVerify(exactly = 1) { repo.getAllEventsByCis(60000001) }
    }

    @Test
    fun `getAllEventsByCis retourne liste vide si aucun`() = runTest {
        // Arrange
        coEvery { repo.getAllEventsByCis(99999999) } returns emptyList()

        // Act
        val result = service.getAllEventsByCis(99999999)

        // Assert
        assertEquals(emptyList(), result)
        coVerify(exactly = 1) { repo.getAllEventsByCis(99999999) }
    }
}
