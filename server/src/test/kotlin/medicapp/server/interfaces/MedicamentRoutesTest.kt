package medicapp.server.interfaces

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.datetime.LocalDate
import medicapp.server.domain.businessclass.medicament.*
import medicapp.server.domain.repository.MedicamentRepository
import medicapp.server.module
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertFalse


object ApiRoutes {
    const val API_BASE = "/api/v1"
    const val MEDICAMENT = "$API_BASE/medicament"

    // Sous-routes Medicament
    const val MEDICAMENT_SPECIALITES = "$MEDICAMENT/specialites"
    const val MEDICAMENT_SUBSTANCES = "$MEDICAMENT/substances"
    const val MEDICAMENT_PRESENTATIONS = "$MEDICAMENT/presentations"
}

class MedicamentRoutesTest {

    private fun ApplicationTestBuilder.configureServerAndGetClient(
        repo: MedicamentRepository = mockk(relaxed = true)
    ): HttpClient {
        application {
            module(repository = repo, enableDatabase = false, enableJobs = false)
        }
        return createClient {
            install(ContentNegotiation) { json() }
        }
    }

    // ─────────────────────────────────────────────────────────────
    // GET /medicament/specialites/{cis}
    // ─────────────────────────────────────────────────────────────

    @Test
    fun `GET specialites par CIS retourne 200 et specialite quand trouve`() = testApplication {
        // Arrange
        val repo = mockk<MedicamentRepository>()
        val expected = MedicamentSpeResume(
            cis = 60000001,
            nom = "DOLIPRANE 1000 mg, comprimé",
            nomOrganisation = "SANOFI AVENTIS FRANCE",
            codeAtc = "N02BE01",
            libelleAtc = "paracétamol",
            voies = listOf("orale")
        )
        coEvery { repo.getSpeResumeByCis(60000001) } returns expected

        val client = configureServerAndGetClient(repo)

        // Act
        val response = client.get("${ApiRoutes.MEDICAMENT_SPECIALITES}/60000001")

        // Assert
        assertEquals(HttpStatusCode.OK, response.status)
        val body = response.body<MedicamentSpeResume>()
        assertEquals(expected, body)
        coVerify(exactly = 1) { repo.getSpeResumeByCis(60000001) }
    }

    @Test
    fun `GET specialites par CIS retourne 404 quand CIS inexistant`() = testApplication {
        // Arrange
        val repo = mockk<MedicamentRepository>()
        coEvery { repo.getSpeResumeByCis(99999999) } returns null

        val client = configureServerAndGetClient(repo)

        // Act
        val response = client.get("${ApiRoutes.MEDICAMENT_SPECIALITES}/99999999")

        // Assert
        assertEquals(HttpStatusCode.NotFound, response.status)
        assertTrue(response.bodyAsText().contains("Aucun médicament"))
    }

    @Test
    fun `GET specialites par CIS retourne 400 quand CIS non-entier`() = testApplication {
        // Arrange
        val client = configureServerAndGetClient()

        // Act
        val response = client.get("${ApiRoutes.MEDICAMENT_SPECIALITES}/abc")

        // Assert
        assertEquals(HttpStatusCode.BadRequest, response.status)
        assertTrue(response.bodyAsText().contains("CIS invalide"))
    }

    @Test
    fun `GET specialites par CIS retourne 400 quand CIS contient caracteres speciaux`() = testApplication {
        // Arrange
        val client = configureServerAndGetClient()

        // Act
        val response = client.get("${ApiRoutes.MEDICAMENT_SPECIALITES}/123abc")

        // Assert
        assertEquals(HttpStatusCode.BadRequest, response.status)
    }

    // ─────────────────────────────────────────────────────────────
    // GET /medicament/specialites?libelle=
    // ─────────────────────────────────────────────────────────────

    @Test
    fun `GET specialites par libelle retourne 200 et liste quand trouve`() = testApplication {
        // Arrange
        val repo = mockk<MedicamentRepository>()
        val expected = listOf(
            MedicamentSpeResume(60000001, "DOLIPRANE", "SANOFI", "N02BE01", "paracétamol", listOf("orale"))
        )
        coEvery { repo.searchSpeResumeByLibelle("doliprane", 10) } returns expected

        val client = configureServerAndGetClient(repo)

        // Act
        val response = client.get("${ApiRoutes.MEDICAMENT_SPECIALITES}?libelle=doliprane")

        // Assert
        assertEquals(HttpStatusCode.OK, response.status)
        val body = response.body<List<MedicamentSpeResume>>()
        assertEquals(expected, body)
    }

    @Test
    fun `GET specialites par libelle retourne 200 et liste vide si service retourne vide`() = testApplication {
        // Arrange
        val repo = mockk<MedicamentRepository>()
        coEvery { repo.searchSpeResumeByLibelle("xyz", 10) } returns emptyList()

        val client = configureServerAndGetClient(repo)

        // Act
        val response = client.get("${ApiRoutes.MEDICAMENT_SPECIALITES}?libelle=xyz")

        // Assert
        assertEquals(HttpStatusCode.OK, response.status)
        val body = response.body<List<MedicamentSpeResume>>()
        assertEquals(emptyList(), body)
    }

    @Test
    fun `GET specialites par libelle retourne 400 si libelle manquant`() = testApplication {
        // Arrange
        val client = configureServerAndGetClient()

        // Act
        val response = client.get(ApiRoutes.MEDICAMENT_SPECIALITES)

        // Assert
        assertEquals(HttpStatusCode.BadRequest, response.status)
        assertTrue(response.bodyAsText().contains("libelle"))
    }

    @Test
    fun `GET specialites par libelle utilise limit par defaut 10`() = testApplication {
        // Arrange
        val repo = mockk<MedicamentRepository>()
        coEvery { repo.searchSpeResumeByLibelle("test", 10) } returns emptyList()

        val client = configureServerAndGetClient(repo)

        // Act
        client.get("${ApiRoutes.MEDICAMENT_SPECIALITES}?libelle=test")

        // Assert
        coVerify(exactly = 1) { repo.searchSpeResumeByLibelle("test", 10) }
    }

    @Test
    fun `GET specialites par libelle utilise limit custom`() = testApplication {
        // Arrange
        val repo = mockk<MedicamentRepository>()
        coEvery { repo.searchSpeResumeByLibelle("test", 25) } returns emptyList()

        val client = configureServerAndGetClient(repo)

        // Act
        client.get("${ApiRoutes.MEDICAMENT_SPECIALITES}?libelle=test&limit=25")

        // Assert
        coVerify(exactly = 1) { repo.searchSpeResumeByLibelle("test", 25) }
    }

    @Test
    fun `GET specialites par libelle ignore limit invalide et utilise defaut`() = testApplication {
        // Arrange
        val repo = mockk<MedicamentRepository>()
        coEvery { repo.searchSpeResumeByLibelle("test", 10) } returns emptyList()

        val client = configureServerAndGetClient(repo)

        // Act
        client.get("${ApiRoutes.MEDICAMENT_SPECIALITES}?libelle=test&limit=abc")

        // Assert
        coVerify(exactly = 1) { repo.searchSpeResumeByLibelle("test", 10) }
    }

    // ─────────────────────────────────────────────────────────────
    // GET /medicament/specialites/completion?libelle=
    // ─────────────────────────────────────────────────────────────

    @Test
    fun `GET specialites completion retourne 200 et completions quand trouve`() = testApplication {
        // Arrange
        val repo = mockk<MedicamentRepository>()
        val expected = listOf(
            MedicamentLibelleCompletion(60000001, "DOLIPRANE 1000 mg")
        )
        coEvery { repo.getLibelleCompletion("doli", 10) } returns expected

        val client = configureServerAndGetClient(repo)

        // Act
        val response = client.get("${ApiRoutes.MEDICAMENT_SPECIALITES}/completion?libelle=doli")

        // Assert
        assertEquals(HttpStatusCode.OK, response.status)
        val body = response.body<List<MedicamentLibelleCompletion>>()
        assertEquals(expected, body)
    }

    @Test
    fun `GET specialites completion retourne 204 quand liste vide`() = testApplication {
        // Arrange
        val repo = mockk<MedicamentRepository>()
        coEvery { repo.getLibelleCompletion("xyz", 10) } returns emptyList()

        val client = configureServerAndGetClient(repo)

        // Act
        val response = client.get("${ApiRoutes.MEDICAMENT_SPECIALITES}/completion?libelle=xyz")

        // Assert
        assertEquals(HttpStatusCode.NoContent, response.status)
    }

    @Test
    fun `GET specialites completion retourne 400 si libelle manquant`() = testApplication {
        // Arrange
        val client = configureServerAndGetClient()

        // Act
        val response = client.get("${ApiRoutes.MEDICAMENT_SPECIALITES}/completion")

        // Assert
        assertEquals(HttpStatusCode.BadRequest, response.status)
    }

    @Test
    fun `GET specialites completion utilise limit par defaut 10`() = testApplication {
        // Arrange
        val repo = mockk<MedicamentRepository>()
        coEvery { repo.getLibelleCompletion("test", 10) } returns emptyList()

        val client = configureServerAndGetClient(repo)

        // Act
        client.get("${ApiRoutes.MEDICAMENT_SPECIALITES}/completion?libelle=test")

        // Assert
        coVerify(exactly = 1) { repo.getLibelleCompletion("test", 10) }
    }

    @Test
    fun `GET specialites completion utilise limit custom`() = testApplication {
        // Arrange
        val repo = mockk<MedicamentRepository>()
        coEvery { repo.getLibelleCompletion("test", 5) } returns emptyList()

        val client = configureServerAndGetClient(repo)

        // Act
        client.get("${ApiRoutes.MEDICAMENT_SPECIALITES}/completion?libelle=test&limit=5")

        // Assert
        coVerify(exactly = 1) { repo.getLibelleCompletion("test", 5) }
    }

    // ─────────────────────────────────────────────────────────────
    // GET /medicament/presentations/cip/{cip13}
    // ─────────────────────────────────────────────────────────────

    @Test
    fun `GET presentation par CIP13 retourne 200 quand trouve`() = testApplication {
        // Arrange
        val repo = mockk<MedicamentRepository>()
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

        val client = configureServerAndGetClient(repo)

        // Act
        val response = client.get("${ApiRoutes.MEDICAMENT_PRESENTATIONS}/cip/3400930000001")

        // Assert
        assertEquals(HttpStatusCode.OK, response.status)
        val body = response.body<MedicamentPresDetail>()
        assertEquals(expected, body)
    }

    @Test
    fun `GET presentation par CIP13 retourne 204 quand non trouve`() = testApplication {
        // Arrange
        val repo = mockk<MedicamentRepository>()
        coEvery { repo.getPresDetailByCip("0000000000000") } returns null

        val client = configureServerAndGetClient(repo)

        // Act
        val response = client.get("${ApiRoutes.MEDICAMENT_PRESENTATIONS}/cip/0000000000000")

        // Assert
        assertEquals(HttpStatusCode.NoContent, response.status)
    }

    @Test
    fun `GET presentation par CIP13 accepte format 13 chiffres`() = testApplication {
        // Arrange
        val repo = mockk<MedicamentRepository>()
        coEvery { repo.getPresDetailByCip("3400930000001") } returns null

        val client = configureServerAndGetClient(repo)

        // Act
        val response = client.get("${ApiRoutes.MEDICAMENT_PRESENTATIONS}/cip/3400930000001")

        // Assert
        coVerify(exactly = 1) { repo.getPresDetailByCip("3400930000001") }
    }

    // ─────────────────────────────────────────────────────────────
    // GET /medicament/presentations/cis/{cis}
    // ─────────────────────────────────────────────────────────────

    @Test
    fun `GET presentations par CIS retourne 200 et liste quand trouve`() = testApplication {
        // Arrange
        val repo = mockk<MedicamentRepository>()
        val expected = listOf(
            MedicamentPresDetail(
                cip13 = "3400930000001",
                label = "DOLIPRANE 1000 mg - boîte de 8",
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
        coEvery { repo.getAllPresDetailByCis(60000001) } returns expected

        val client = configureServerAndGetClient(repo)

        // Act
        val response = client.get("${ApiRoutes.MEDICAMENT_PRESENTATIONS}/cis/60000001")

        // Assert
        assertEquals(HttpStatusCode.OK, response.status)
        val body = response.body<List<MedicamentPresDetail>>()
        assertEquals(expected, body)
    }

    @Test
    fun `GET presentations par CIS retourne 204 si liste vide`() = testApplication {
        // Arrange
        val repo = mockk<MedicamentRepository>()
        coEvery { repo.getAllPresDetailByCis(99999999) } returns emptyList()

        val client = configureServerAndGetClient(repo)

        // Act
        val response = client.get("${ApiRoutes.MEDICAMENT_PRESENTATIONS}/cis/99999999")

        // Assert
        assertEquals(HttpStatusCode.NoContent, response.status)
    }

    @Test
    fun `GET presentations par CIS retourne 400 si CIS invalide`() = testApplication {
        // Arrange
        val client = configureServerAndGetClient()

        // Act
        val response = client.get("${ApiRoutes.MEDICAMENT_PRESENTATIONS}/cis/abc")

        // Assert
        assertEquals(HttpStatusCode.BadRequest, response.status)
    }

    @Test
    fun `GET presentations par CIS retourne liste multiple`() = testApplication {
        // Arrange
        val repo = mockk<MedicamentRepository>()
        val expected = listOf(
            MedicamentPresDetail("3400930000001", "Boîte 8", "DOLIPRANE", 60000001, "SANOFI", "N02BE01", "paracétamol", listOf("orale"), 8, null, null, null),
            MedicamentPresDetail("3400930000002", "Boîte 16", "DOLIPRANE", 60000001, "SANOFI", "N02BE01", "paracétamol", listOf("orale"), 16, null, null, null)
        )
        coEvery { repo.getAllPresDetailByCis(60000001) } returns expected

        val client = configureServerAndGetClient(repo)

        // Act
        val response = client.get("${ApiRoutes.MEDICAMENT_PRESENTATIONS}/cis/60000001")

        // Assert
        assertEquals(HttpStatusCode.OK, response.status)
        val body = response.body<List<MedicamentPresDetail>>()
        assertEquals(2, body.size)
    }

    // ─────────────────────────────────────────────────────────────
    // GET /medicament/presentations?libelle=
    // ─────────────────────────────────────────────────────────────

    @Test
    fun `GET presentations par libelle retourne 200 et liste quand trouve`() = testApplication {
        // Arrange
        val repo = mockk<MedicamentRepository>()
        val expected = listOf(
            MedicamentPresDetail("3400930000001", "DOLIPRANE", "DOLIPRANE", 60000001, "SANOFI", "N02BE01", "paracétamol", listOf("orale"), 8, null, null, null)
        )
        coEvery { repo.searchPresDetailByLibelle("doliprane", 10) } returns expected

        val client = configureServerAndGetClient(repo)

        // Act
        val response = client.get("${ApiRoutes.MEDICAMENT_PRESENTATIONS}?libelle=doliprane")

        // Assert
        assertEquals(HttpStatusCode.OK, response.status)
        val body = response.body<List<MedicamentPresDetail>>()
        assertEquals(expected, body)
    }

    @Test
    fun `GET presentations par libelle retourne 204 si liste vide`() = testApplication {
        // Arrange
        val repo = mockk<MedicamentRepository>()
        coEvery { repo.searchPresDetailByLibelle("xyz", 10) } returns emptyList()

        val client = configureServerAndGetClient(repo)

        // Act
        val response = client.get("${ApiRoutes.MEDICAMENT_PRESENTATIONS}?libelle=xyz")

        // Assert
        assertEquals(HttpStatusCode.NoContent, response.status)
    }

    @Test
    fun `GET presentations par libelle retourne 400 si libelle manquant`() = testApplication {
        // Arrange
        val client = configureServerAndGetClient()

        // Act
        val response = client.get(ApiRoutes.MEDICAMENT_PRESENTATIONS)

        // Assert
        assertEquals(HttpStatusCode.BadRequest, response.status)
        assertTrue(response.bodyAsText().contains("libelle"))
    }

    @Test
    fun `GET presentations par libelle utilise limit par defaut 10`() = testApplication {
        // Arrange
        val repo = mockk<MedicamentRepository>()
        coEvery { repo.searchPresDetailByLibelle("test", 10) } returns emptyList()

        val client = configureServerAndGetClient(repo)

        // Act
        client.get("${ApiRoutes.MEDICAMENT_PRESENTATIONS}?libelle=test")

        // Assert
        coVerify(exactly = 1) { repo.searchPresDetailByLibelle("test", 10) }
    }

    @Test
    fun `GET presentations par libelle utilise limit custom`() = testApplication {
        // Arrange
        val repo = mockk<MedicamentRepository>()
        coEvery { repo.searchPresDetailByLibelle("test", 20) } returns emptyList()

        val client = configureServerAndGetClient(repo)

        // Act
        client.get("${ApiRoutes.MEDICAMENT_PRESENTATIONS}?libelle=test&limit=20")

        // Assert
        coVerify(exactly = 1) { repo.searchPresDetailByLibelle("test", 20) }
    }

    // ─────────────────────────────────────────────────────────────
    // GET /medicament/substances/resume/cis/{cis}
    // ─────────────────────────────────────────────────────────────

    @Test
    fun `GET substances resume par CIS retourne 200 et liste quand trouve`() = testApplication {
        // Arrange
        val repo = mockk<MedicamentRepository>()
        val expected = listOf(
            MedicamentSubstanceResume("2065", "PARACÉTAMOL", "principe actif", "1000 mg", 1, null)
        )
        coEvery { repo.getSubstancesResumeByCis(60000001) } returns expected

        val client = configureServerAndGetClient(repo)

        // Act
        val response = client.get("${ApiRoutes.MEDICAMENT_SUBSTANCES}/resume/cis/60000001")

        // Assert
        assertEquals(HttpStatusCode.OK, response.status)
        val body = response.body<List<MedicamentSubstanceResume>>()
        assertEquals(expected, body)
    }

    @Test
    fun `GET substances resume par CIS retourne 204 si liste vide`() = testApplication {
        // Arrange
        val repo = mockk<MedicamentRepository>()
        coEvery { repo.getSubstancesResumeByCis(99999999) } returns emptyList()

        val client = configureServerAndGetClient(repo)

        // Act
        val response = client.get("${ApiRoutes.MEDICAMENT_SUBSTANCES}/resume/cis/99999999")

        // Assert
        assertEquals(HttpStatusCode.NoContent, response.status)
    }

    @Test
    fun `GET substances resume par CIS retourne 400 si CIS invalide`() = testApplication {
        // Arrange
        val client = configureServerAndGetClient()

        // Act
        val response = client.get("${ApiRoutes.MEDICAMENT_SUBSTANCES}/resume/cis/abc")

        // Assert
        assertEquals(HttpStatusCode.BadRequest, response.status)
    }

    @Test
    fun `GET substances resume par CIS retourne liste multiple`() = testApplication {
        // Arrange
        val repo = mockk<MedicamentRepository>()
        val expected = listOf(
            MedicamentSubstanceResume("2065", "PARACÉTAMOL", "principe actif", "1000 mg", 1, null),
            MedicamentSubstanceResume("1234", "CAFÉINE", "excipient", "50 mg", null, null)
        )
        coEvery { repo.getSubstancesResumeByCis(60000001) } returns expected

        val client = configureServerAndGetClient(repo)

        // Act
        val response = client.get("${ApiRoutes.MEDICAMENT_SUBSTANCES}/resume/cis/60000001")

        // Assert
        assertEquals(HttpStatusCode.OK, response.status)
        val body = response.body<List<MedicamentSubstanceResume>>()
        assertEquals(2, body.size)
    }

    // ─────────────────────────────────────────────────────────────
    // GET /medicament/substances/resume?cis=&codeSubstance=
    // ─────────────────────────────────────────────────────────────

    @Test
    fun `GET substance resume par CIS et code retourne 200 quand trouve`() = testApplication {
        // Arrange
        val repo = mockk<MedicamentRepository>()
        val expected = MedicamentSubstanceResume("2065", "PARACÉTAMOL", "principe actif", "1000 mg", 1, null)
        coEvery { repo.getSubstanceResumeByCisAndCode(60000001, "2065") } returns expected

        val client = configureServerAndGetClient(repo)

        // Act
        val response = client.get("${ApiRoutes.MEDICAMENT_SUBSTANCES}/resume?cis=60000001&codeSubstance=2065")

        // Assert
        assertEquals(HttpStatusCode.OK, response.status)
        val body = response.body<MedicamentSubstanceResume>()
        assertEquals(expected, body)
    }

    @Test
    fun `GET substance resume par CIS et code retourne 204 si non trouve`() = testApplication {
        // Arrange
        val repo = mockk<MedicamentRepository>()
        coEvery { repo.getSubstanceResumeByCisAndCode(60000001, "9999") } returns null

        val client = configureServerAndGetClient(repo)

        // Act
        val response = client.get("${ApiRoutes.MEDICAMENT_SUBSTANCES}/resume?cis=60000001&codeSubstance=9999")

        // Assert
        assertEquals(HttpStatusCode.NoContent, response.status)
    }

    @Test
    fun `GET substance resume retourne 400 si codeSubstance manquant`() = testApplication {
        // Arrange
        val client = configureServerAndGetClient()

        // Act
        val response = client.get("${ApiRoutes.MEDICAMENT_SUBSTANCES}/resume?cis=60000001")

        // Assert
        assertEquals(HttpStatusCode.BadRequest, response.status)
    }

    @Test
    fun `GET substance resume retourne 400 si cis manquant`() = testApplication {
        // Arrange
        val client = configureServerAndGetClient()

        // Act
        val response = client.get("${ApiRoutes.MEDICAMENT_SUBSTANCES}/resume?codeSubstance=2065")

        // Assert
        assertEquals(HttpStatusCode.BadRequest, response.status)
    }

    @Test
    fun `GET substance resume retourne 400 si cis invalide`() = testApplication {
        // Arrange
        val client = configureServerAndGetClient()

        // Act
        val response = client.get("${ApiRoutes.MEDICAMENT_SUBSTANCES}/resume?cis=abc&codeSubstance=2065")

        // Assert
        assertEquals(HttpStatusCode.BadRequest, response.status)
    }

    // ─────────────────────────────────────────────────────────────
    // GET /medicament/substances/detail/cis/{cis}
    // ─────────────────────────────────────────────────────────────

    @Test
    fun `GET substances detail par CIS retourne 200 et liste quand trouve`() = testApplication {
        // Arrange
        val repo = mockk<MedicamentRepository>()
        val expected = listOf(
            MedicamentSubstanceDetail("PARACÉTAMOL", "principe actif", "C8H9NO2", 151.16f)
        )
        coEvery { repo.getSubstancesDetailByCis(60000001) } returns expected

        val client = configureServerAndGetClient(repo)

        // Act
        val response = client.get("${ApiRoutes.MEDICAMENT_SUBSTANCES}/detail/cis/60000001")

        // Assert
        assertEquals(HttpStatusCode.OK, response.status)
        val body = response.body<List<MedicamentSubstanceDetail>>()
        assertEquals(expected, body)
    }

    @Test
    fun `GET substances detail par CIS retourne 204 si liste vide`() = testApplication {
        // Arrange
        val repo = mockk<MedicamentRepository>()
        coEvery { repo.getSubstancesDetailByCis(99999999) } returns emptyList()

        val client = configureServerAndGetClient(repo)

        // Act
        val response = client.get("${ApiRoutes.MEDICAMENT_SUBSTANCES}/detail/cis/99999999")

        // Assert
        assertEquals(HttpStatusCode.NoContent, response.status)
    }

    @Test
    fun `GET substances detail par CIS retourne 400 si CIS invalide`() = testApplication {
        // Arrange
        val client = configureServerAndGetClient()

        // Act
        val response = client.get("${ApiRoutes.MEDICAMENT_SUBSTANCES}/detail/cis/abc")

        // Assert
        assertEquals(HttpStatusCode.BadRequest, response.status)
    }

    // ─────────────────────────────────────────────────────────────
    // GET /medicament/substances/detail?cis=&codeSubstance=
    // ─────────────────────────────────────────────────────────────

    @Test
    fun `GET substance detail par CIS et code retourne 200 quand trouve`() = testApplication {
        // Arrange
        val repo = mockk<MedicamentRepository>()
        val expected = MedicamentSubstanceDetail("PARACÉTAMOL", "principe actif", "C8H9NO2", 151.16f)
        coEvery { repo.getSubstanceDetailByCisAndCode(60000001, "2065") } returns expected

        val client = configureServerAndGetClient(repo)

        // Act
        val response = client.get("${ApiRoutes.MEDICAMENT_SUBSTANCES}/detail?cis=60000001&codeSubstance=2065")

        // Assert
        assertEquals(HttpStatusCode.OK, response.status)
        val body = response.body<MedicamentSubstanceDetail>()
        assertEquals(expected, body)
    }

    @Test
    fun `GET substance detail par CIS et code retourne 204 si non trouve`() = testApplication {
        // Arrange
        val repo = mockk<MedicamentRepository>()
        coEvery { repo.getSubstanceDetailByCisAndCode(60000001, "9999") } returns null

        val client = configureServerAndGetClient(repo)

        // Act
        val response = client.get("${ApiRoutes.MEDICAMENT_SUBSTANCES}/detail?cis=60000001&codeSubstance=9999")

        // Assert
        assertEquals(HttpStatusCode.NoContent, response.status)
    }

    @Test
    fun `GET substance detail retourne 400 si codeSubstance manquant`() = testApplication {
        // Arrange
        val client = configureServerAndGetClient()

        // Act
        val response = client.get("${ApiRoutes.MEDICAMENT_SUBSTANCES}/detail?cis=60000001")

        // Assert
        assertEquals(HttpStatusCode.BadRequest, response.status)
    }

    @Test
    fun `GET substance detail retourne 400 si cis manquant`() = testApplication {
        // Arrange
        val client = configureServerAndGetClient()

        // Act
        val response = client.get("${ApiRoutes.MEDICAMENT_SUBSTANCES}/detail?codeSubstance=2065")

        // Assert
        assertEquals(HttpStatusCode.BadRequest, response.status)
    }

    @Test
    fun `GET substance detail retourne 400 si cis invalide`() = testApplication {
        // Arrange
        val client = configureServerAndGetClient()

        // Act
        val response = client.get("${ApiRoutes.MEDICAMENT_SUBSTANCES}/detail?cis=abc&codeSubstance=2065")

        // Assert
        assertEquals(HttpStatusCode.BadRequest, response.status)
    }

    // ─────────────────────────────────────────────────────────────
    // GET /medicament/presentations/status/latest/{cip13}
    // ─────────────────────────────────────────────────────────────

    @Test
    fun `GET latest presentation status retourne 200 quand trouve`() = testApplication {
        // Arrange
        val repo = mockk<MedicamentRepository>()
        val expected = MedicamentPresStatus("3400930000001", LocalDate(2024, 1, 15), "Commercialisation")
        coEvery { repo.getLatestEventByCip("3400930000001") } returns expected

        val client = configureServerAndGetClient(repo)

        // Act
        val response = client.get("${ApiRoutes.MEDICAMENT_PRESENTATIONS}/status/latest/3400930000001")

        // Assert
        assertEquals(HttpStatusCode.OK, response.status)
        val body = response.body<MedicamentPresStatus>()
        assertEquals(expected, body)
    }

    @Test
    fun `GET latest presentation status retourne 204 si non trouve`() = testApplication {
        // Arrange
        val repo = mockk<MedicamentRepository>()
        coEvery { repo.getLatestEventByCip("0000000000000") } returns null

        val client = configureServerAndGetClient(repo)

        // Act
        val response = client.get("${ApiRoutes.MEDICAMENT_PRESENTATIONS}/status/latest/0000000000000")

        // Assert
        assertEquals(HttpStatusCode.NoContent, response.status)
    }

    @Test
    fun `GET latest presentation status verifie champs status`() = testApplication {
        // Arrange
        val repo = mockk<MedicamentRepository>()
        val expected = MedicamentPresStatus("3400930000001", LocalDate(2024, 6, 15), "Arrêt de commercialisation")
        coEvery { repo.getLatestEventByCip("3400930000001") } returns expected

        val client = configureServerAndGetClient(repo)

        // Act
        val response = client.get("${ApiRoutes.MEDICAMENT_PRESENTATIONS}/status/latest/3400930000001")

        // Assert
        assertEquals(HttpStatusCode.OK, response.status)
        val body = response.body<MedicamentPresStatus>()
        assertEquals("3400930000001", body.cip13)
        assertEquals(LocalDate(2024, 6, 15), body.dateEffet)
        assertEquals("Arrêt de commercialisation", body.typeEvenement)
    }

    // ─────────────────────────────────────────────────────────────
    // GET /medicament/presentations/status/all/{cip13}
    // ─────────────────────────────────────────────────────────────

    @Test
    fun `GET all presentation statuses retourne 200 quand trouve`() = testApplication {
        // Arrange
        val repo = mockk<MedicamentRepository>()
        val expected = listOf(
            MedicamentPresStatus("3400930000001", LocalDate(2024, 1, 15), "Commercialisation")
        )
        coEvery { repo.getAllEventsByCip("3400930000001") } returns expected

        val client = configureServerAndGetClient(repo)

        // Act
        val response = client.get("${ApiRoutes.MEDICAMENT_PRESENTATIONS}/status/all/3400930000001")

        // Assert
        assertEquals(HttpStatusCode.OK, response.status)
        val body = response.body<List<MedicamentPresStatus>>()
        assertEquals(expected, body)
    }

    @Test
    fun `GET all presentation statuses retourne 204 si liste vide`() = testApplication {
        // Arrange
        val repo = mockk<MedicamentRepository>()
        coEvery { repo.getAllEventsByCip("0000000000000") } returns emptyList()

        val client = configureServerAndGetClient(repo)

        // Act
        val response = client.get("${ApiRoutes.MEDICAMENT_PRESENTATIONS}/status/all/0000000000000")

        // Assert
        assertEquals(HttpStatusCode.NoContent, response.status)
    }

    @Test
    fun `GET all presentation statuses retourne liste multiple`() = testApplication {
        // Arrange
        val repo = mockk<MedicamentRepository>()
        val expected = listOf(
            MedicamentPresStatus("3400930000001", LocalDate(2024, 1, 15), "Commercialisation"),
            MedicamentPresStatus("3400930000001", LocalDate(2020, 6, 1), "Autorisation")
        )
        coEvery { repo.getAllEventsByCip("3400930000001") } returns expected

        val client = configureServerAndGetClient(repo)

        // Act
        val response = client.get("${ApiRoutes.MEDICAMENT_PRESENTATIONS}/status/all/3400930000001")

        // Assert
        assertEquals(HttpStatusCode.OK, response.status)
        val body = response.body<List<MedicamentPresStatus>>()
        assertEquals(2, body.size)
    }

    // ─────────────────────────────────────────────────────────────
    // GET /medicament/presentations/status/marketed/{cip13}
    // ─────────────────────────────────────────────────────────────

    @Test
    fun `GET presentation marketed retourne 200 avec true`() = testApplication {
        // Arrange
        val repo = mockk<MedicamentRepository>()
        coEvery { repo.isPresMarketed("3400930000001") } returns true

        val client = configureServerAndGetClient(repo)

        // Act
        val response = client.get("${ApiRoutes.MEDICAMENT_PRESENTATIONS}/status/marketed/3400930000001")

        // Assert
        assertEquals(HttpStatusCode.OK, response.status)
        val body = response.body<Boolean>()
        assertTrue(body)
    }

    @Test
    fun `GET presentation marketed retourne 200 avec false`() = testApplication {
        // Arrange
        val repo = mockk<MedicamentRepository>()
        coEvery { repo.isPresMarketed("0000000000000") } returns false

        val client = configureServerAndGetClient(repo)

        // Act
        val response = client.get("${ApiRoutes.MEDICAMENT_PRESENTATIONS}/status/marketed/0000000000000")

        // Assert
        assertEquals(HttpStatusCode.OK, response.status)
        val body = response.body<Boolean>()
        assertFalse(body)
    }

    // ─────────────────────────────────────────────────────────────
    // GET /medicament/specialites/status/latest/{cis}
    // ─────────────────────────────────────────────────────────────

    @Test
    fun `GET latest specialite status retourne 200 quand trouve`() = testApplication {
        // Arrange
        val repo = mockk<MedicamentRepository>()
        val expected = MedicamentSpeStatus(60000001, LocalDate(2024, 1, 15), "Commercialisation")
        coEvery { repo.getLatestEventByCis(60000001) } returns expected

        val client = configureServerAndGetClient(repo)

        // Act
        val response = client.get("${ApiRoutes.MEDICAMENT_SPECIALITES}/status/latest/60000001")

        // Assert
        assertEquals(HttpStatusCode.OK, response.status)
        val body = response.body<MedicamentSpeStatus>()
        assertEquals(expected, body)
    }

    @Test
    fun `GET latest specialite status retourne 204 si non trouve`() = testApplication {
        // Arrange
        val repo = mockk<MedicamentRepository>()
        coEvery { repo.getLatestEventByCis(99999999) } returns null

        val client = configureServerAndGetClient(repo)

        // Act
        val response = client.get("${ApiRoutes.MEDICAMENT_SPECIALITES}/status/latest/99999999")

        // Assert
        assertEquals(HttpStatusCode.NoContent, response.status)
    }

    @Test
    fun `GET latest specialite status retourne 400 si CIS invalide`() = testApplication {
        // Arrange
        val client = configureServerAndGetClient()

        // Act
        val response = client.get("${ApiRoutes.MEDICAMENT_SPECIALITES}/status/latest/abc")

        // Assert
        assertEquals(HttpStatusCode.BadRequest, response.status)
    }

    @Test
    fun `GET latest specialite status verifie champs status`() = testApplication {
        // Arrange
        val repo = mockk<MedicamentRepository>()
        val expected = MedicamentSpeStatus(60000001, LocalDate(2024, 6, 15), "Suspension AMM")
        coEvery { repo.getLatestEventByCis(60000001) } returns expected

        val client = configureServerAndGetClient(repo)

        // Act
        val response = client.get("${ApiRoutes.MEDICAMENT_SPECIALITES}/status/latest/60000001")

        // Assert
        assertEquals(HttpStatusCode.OK, response.status)
        val body = response.body<MedicamentSpeStatus>()
        assertEquals(60000001, body.cis)
        assertEquals(LocalDate(2024, 6, 15), body.dateEffet)
        assertEquals("Suspension AMM", body.typeEvenement)
    }

    // ─────────────────────────────────────────────────────────────
    // GET /medicament/specialites/status/all/{cis}
    // ─────────────────────────────────────────────────────────────

    @Test
    fun `GET all specialite statuses retourne 200 quand trouve`() = testApplication {
        // Arrange
        val repo = mockk<MedicamentRepository>()
        val expected = listOf(
            MedicamentSpeStatus(60000001, LocalDate(2024, 1, 15), "Commercialisation")
        )
        coEvery { repo.getAllEventsByCis(60000001) } returns expected

        val client = configureServerAndGetClient(repo)

        // Act
        val response = client.get("${ApiRoutes.MEDICAMENT_SPECIALITES}/status/all/60000001")

        // Assert
        assertEquals(HttpStatusCode.OK, response.status)
        val body = response.body<List<MedicamentSpeStatus>>()
        assertEquals(expected, body)
    }

    @Test
    fun `GET all specialite statuses retourne 204 si liste vide`() = testApplication {
        // Arrange
        val repo = mockk<MedicamentRepository>()
        coEvery { repo.getAllEventsByCis(99999999) } returns emptyList()

        val client = configureServerAndGetClient(repo)

        // Act
        val response = client.get("${ApiRoutes.MEDICAMENT_SPECIALITES}/status/all/99999999")

        // Assert
        assertEquals(HttpStatusCode.NoContent, response.status)
    }

    @Test
    fun `GET all specialite statuses retourne 400 si CIS invalide`() = testApplication {
        // Arrange
        val client = configureServerAndGetClient()

        // Act
        val response = client.get("${ApiRoutes.MEDICAMENT_SPECIALITES}/status/all/abc")

        // Assert
        assertEquals(HttpStatusCode.BadRequest, response.status)
    }

    @Test
    fun `GET all specialite statuses retourne liste multiple`() = testApplication {
        // Arrange
        val repo = mockk<MedicamentRepository>()
        val expected = listOf(
            MedicamentSpeStatus(60000001, LocalDate(2024, 1, 15), "Commercialisation"),
            MedicamentSpeStatus(60000001, LocalDate(2020, 6, 1), "Autorisation")
        )
        coEvery { repo.getAllEventsByCis(60000001) } returns expected

        val client = configureServerAndGetClient(repo)

        // Act
        val response = client.get("${ApiRoutes.MEDICAMENT_SPECIALITES}/status/all/60000001")

        // Assert
        assertEquals(HttpStatusCode.OK, response.status)
        val body = response.body<List<MedicamentSpeStatus>>()
        assertEquals(2, body.size)
    }

    // ─────────────────────────────────────────────────────────────
    // GET /medicament/specialites/status/marketed/{cis}
    // ─────────────────────────────────────────────────────────────

    @Test
    fun `GET specialite marketed retourne 200 avec true`() = testApplication {
        // Arrange
        val repo = mockk<MedicamentRepository>()
        coEvery { repo.isSpeMarketed(60000001) } returns true

        val client = configureServerAndGetClient(repo)

        // Act
        val response = client.get("${ApiRoutes.MEDICAMENT_SPECIALITES}/status/marketed/60000001")

        // Assert
        assertEquals(HttpStatusCode.OK, response.status)
        val body = response.body<Boolean>()
        assertTrue(body)
    }

    @Test
    fun `GET specialite marketed retourne 200 avec false`() = testApplication {
        // Arrange
        val repo = mockk<MedicamentRepository>()
        coEvery { repo.isSpeMarketed(99999999) } returns false

        val client = configureServerAndGetClient(repo)

        // Act
        val response = client.get("${ApiRoutes.MEDICAMENT_SPECIALITES}/status/marketed/99999999")

        // Assert
        assertEquals(HttpStatusCode.OK, response.status)
        val body = response.body<Boolean>()
        assertFalse(body)
    }

    @Test
    fun `GET specialite marketed retourne 400 si CIS invalide`() = testApplication {
        // Arrange
        val client = configureServerAndGetClient()

        // Act
        val response = client.get("${ApiRoutes.MEDICAMENT_SPECIALITES}/status/marketed/abc")

        // Assert
        assertEquals(HttpStatusCode.BadRequest, response.status)
    }
}
