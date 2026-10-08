package medicapp.server.interfaces

import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.config.MapApplicationConfig
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import medicapp.server.domain.repository.MedicamentRepository
import medicapp.server.module
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/** Durcissement de l'API : plafond de `limit`, limitation de débit, /health réel, CORS sans credentials. */
class ApiHardeningTest {

    private val searchUrl = "${ApiRoutes.MEDICAMENT_SPECIALITES}?libelle=x"

    private fun ApplicationTestBuilder.client(
        repo: MedicamentRepository = mockk(relaxed = true),
        config: MapApplicationConfig = MapApplicationConfig(),
        healthCheck: (suspend () -> Boolean)? = null
    ): HttpClient {
        application {
            module(
                config = config,
                repository = repo,
                enableDatabase = false,
                enableJobs = false,
                healthCheck = healthCheck
            )
        }
        return createClient { install(ContentNegotiation) { json() } }
    }

    // ── limit ──────────────────────────────────────────────────

    @Test
    fun `limit trop grand est ramene au maximum`() = testApplication {
        val repo = mockk<MedicamentRepository>()
        coEvery { repo.searchSpeResumeByLibelle("x", MAX_LIMIT) } returns emptyList()

        val response = client(repo).get("$searchUrl&limit=1000000")

        assertEquals(HttpStatusCode.OK, response.status)
        coVerify(exactly = 1) { repo.searchSpeResumeByLibelle("x", MAX_LIMIT) }
    }

    @Test
    fun `limit nul ou negatif est ramene a 1`() = testApplication {
        val repo = mockk<MedicamentRepository>()
        coEvery { repo.searchSpeResumeByLibelle("x", 1) } returns emptyList()
        val client = client(repo)

        client.get("$searchUrl&limit=0")
        client.get("$searchUrl&limit=-5")

        coVerify(exactly = 2) { repo.searchSpeResumeByLibelle("x", 1) }
    }

    @Test
    fun `limit invalide ou absent utilise la valeur par defaut`() = testApplication {
        val repo = mockk<MedicamentRepository>()
        coEvery { repo.searchSpeResumeByLibelle("x", DEFAULT_LIMIT) } returns emptyList()
        val client = client(repo)

        client.get("$searchUrl&limit=abc")
        client.get(searchUrl)

        coVerify(exactly = 2) { repo.searchSpeResumeByLibelle("x", DEFAULT_LIMIT) }
    }

    // ── limitation de débit ────────────────────────────────────

    @Test
    fun `au dela du quota par minute l API repond 429 avec Retry-After`() = testApplication {
        val client = client(config = MapApplicationConfig("api.rate-limit.per-minute" to "3"))

        repeat(3) {
            assertEquals(HttpStatusCode.OK, client.get(searchUrl).status, "requête ${it + 1} dans le quota")
        }
        val rejected = client.get(searchUrl)

        assertEquals(HttpStatusCode.TooManyRequests, rejected.status)
        assertNotNull(rejected.headers[HttpHeaders.RetryAfter])
    }

    @Test
    fun `le health check n est pas limite en debit`() = testApplication {
        val client = client(config = MapApplicationConfig("api.rate-limit.per-minute" to "1"))

        repeat(5) { assertEquals(HttpStatusCode.OK, client.get("/health").status) }
    }

    // ── /health ────────────────────────────────────────────────

    @Test
    fun `health repond 200 quand la base repond`() = testApplication {
        val response = client(healthCheck = { true }).get("/health")

        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals("OK", response.bodyAsText())
    }

    @Test
    fun `health repond 503 quand la base ne repond pas`() = testApplication {
        val response = client(healthCheck = { false }).get("/health")

        assertEquals(HttpStatusCode.ServiceUnavailable, response.status)
    }

    // ── CORS ───────────────────────────────────────────────────

    @Test
    fun `CORS n autorise pas l envoi de credentials`() = testApplication {
        val response = client().get(searchUrl) { headers.append(HttpHeaders.Origin, "https://example.org") }

        assertNull(response.headers[HttpHeaders.AccessControlAllowCredentials])
    }
}
