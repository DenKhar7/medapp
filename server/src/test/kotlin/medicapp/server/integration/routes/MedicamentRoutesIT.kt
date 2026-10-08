package medicapp.server.integration.routes

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import medicapp.server.domain.businessclass.medicament.MedicamentLibelleCompletion
import medicapp.server.domain.businessclass.medicament.MedicamentPresDetail
import medicapp.server.domain.businessclass.medicament.MedicamentPresStatus
import medicapp.server.domain.businessclass.medicament.MedicamentSpeResume
import medicapp.server.domain.businessclass.medicament.MedicamentSpeStatus
import medicapp.server.domain.businessclass.medicament.MedicamentSubstanceDetail
import medicapp.server.domain.businessclass.medicament.MedicamentSubstanceResume
import medicapp.server.infrastructure.db.requests.repository.MedicamentRepositoryMariaDb
import medicapp.server.integration.config.IntegrationTestExtension
import medicapp.server.integration.fixtures.TestFixtures
import medicapp.server.module
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Tests d'intégration bout-en-bout pour MedicamentRoutes.
 *
 * Ces tests valident le flux complet :
 * Requête HTTP -> Routes Ktor -> MedicamentService -> MedicamentRepositoryMariaDb -> MariaDB -> Réponse JSON
 *
 * Utilise le vrai repository avec la base de données de test (pas de mocks).
 */
@ExtendWith(IntegrationTestExtension::class)
class MedicamentRoutesIT {

    private object ApiRoutes {
        const val API_BASE = "/api/v1"
        const val MEDICAMENT = "$API_BASE/medicament"
        const val MEDICAMENT_SPECIALITES = "$MEDICAMENT/specialites"
        const val MEDICAMENT_SUBSTANCES = "$MEDICAMENT/substances"
        const val MEDICAMENT_PRESENTATIONS = "$MEDICAMENT/presentations"
    }

    /**
     * Configure l'application de test Ktor avec le vrai repository.
     * La base de données est déjà configurée par IntegrationTestExtension.
     */
    private fun ApplicationTestBuilder.configureRealServerAndGetClient(): HttpClient {
        application {
            // Utiliser le vrai repository - la base de données est configurée par l'extension
            module(
                repository = MedicamentRepositoryMariaDb(),
                enableDatabase = false, // Déjà configuré par IntegrationTestExtension
                enableJobs = false
            )
        }
        return createClient {
            install(ContentNegotiation) { json() }
        }
    }

    // ─────────────────────────────────────────────────────────────
    // GET /medicament/specialites/{cis}
    // ─────────────────────────────────────────────────────────────

    @Test
    fun `GET specialites by CIS returns 200 with complete JSON from real database`() = testApplication {
        // Préparation
        val client = configureRealServerAndGetClient()
        val cis = TestFixtures.OxomemazineActive.CIS

        // Action
        val response = client.get("${ApiRoutes.MEDICAMENT_SPECIALITES}/$cis")

        // Vérification
        assertEquals(HttpStatusCode.OK, response.status)
        val body = response.body<MedicamentSpeResume>()
        assertEquals(cis, body.cis)
        assertTrue(body.nom.contains("OXOMEMAZINE"))
        assertEquals(TestFixtures.OxomemazineActive.CODE_ATC, body.codeAtc)
        assertEquals(TestFixtures.OxomemazineActive.LIBELLE_ATC, body.libelleAtc)
        assertEquals(TestFixtures.OxomemazineActive.ORGANISATION, body.nomOrganisation)
        assertTrue(body.voies.isNotEmpty())
    }

    @Test
    fun `GET specialites by CIS returns 404 for non-existent CIS`() = testApplication {
        // Préparation
        val client = configureRealServerAndGetClient()
        val nonExistentCis = TestFixtures.CIS_INEXISTANT

        // Action
        val response = client.get("${ApiRoutes.MEDICAMENT_SPECIALITES}/$nonExistentCis")

        // Vérification
        assertEquals(HttpStatusCode.NotFound, response.status)
        assertTrue(response.bodyAsText().contains("médicament", ignoreCase = true))
    }

    @Test
    fun `GET specialites by CIS returns 400 for invalid CIS format`() = testApplication {
        // Préparation
        val client = configureRealServerAndGetClient()

        // Action
        val response = client.get("${ApiRoutes.MEDICAMENT_SPECIALITES}/invalid")

        // Vérification
        assertEquals(HttpStatusCode.BadRequest, response.status)
    }

    // ─────────────────────────────────────────────────────────────
    // GET /medicament/specialites?libelle=
    // ─────────────────────────────────────────────────────────────

    @Test
    fun `GET specialites search returns results from real database`() = testApplication {
        // Préparation
        val client = configureRealServerAndGetClient()

        // Action
        val response = client.get("${ApiRoutes.MEDICAMENT_SPECIALITES}?libelle=OXOMEMAZINE&limit=5")

        // Vérification
        assertEquals(HttpStatusCode.OK, response.status)
        val body = response.body<List<MedicamentSpeResume>>()
        assertTrue(body.isNotEmpty())
        assertTrue(body.all { it.nom.contains("OXOMEMAZINE", ignoreCase = true) })
    }

    @Test
    fun `GET specialites search returns empty list for no matches`() = testApplication {
        // Préparation
        val client = configureRealServerAndGetClient()

        // Action
        val response = client.get("${ApiRoutes.MEDICAMENT_SPECIALITES}?libelle=ZZZZNONEXISTENT&limit=10")

        // Vérification
        assertEquals(HttpStatusCode.OK, response.status)
        val body = response.body<List<MedicamentSpeResume>>()
        assertTrue(body.isEmpty())
    }

    // ─────────────────────────────────────────────────────────────
    // GET /medicament/specialites/completion?libelle=
    // ─────────────────────────────────────────────────────────────

    @Test
    fun `GET specialites completion returns suggestions from real database`() = testApplication {
        // Préparation
        val client = configureRealServerAndGetClient()

        // Action
        val response = client.get("${ApiRoutes.MEDICAMENT_SPECIALITES}/completion?libelle=OXOME&limit=5")

        // Vérification
        assertEquals(HttpStatusCode.OK, response.status)
        val body = response.body<List<MedicamentLibelleCompletion>>()
        assertTrue(body.isNotEmpty())
    }

    // ─────────────────────────────────────────────────────────────
    // GET /medicament/presentations/cip/{cip13}
    // ─────────────────────────────────────────────────────────────

    @Test
    fun `GET presentations by CIP returns 200 with complete joined data`() = testApplication {
        // Préparation
        val client = configureRealServerAndGetClient()
        val cip13 = TestFixtures.OxomemazinePresentation.CIP13

        // Action
        val response = client.get("${ApiRoutes.MEDICAMENT_PRESENTATIONS}/cip/$cip13")

        // Vérification
        assertEquals(HttpStatusCode.OK, response.status)
        val body = response.body<MedicamentPresDetail>()
        assertEquals(cip13, body.cip13)
        assertEquals(TestFixtures.OxomemazinePresentation.CIS, body.cis)
        assertTrue(body.label.contains("OXOMEMAZINE"))
        assertEquals(TestFixtures.OxomemazinePresentation.QUANTITE_CONDITIONNEMENT, body.quantiteConditionnement)
        assertEquals(TestFixtures.OxomemazinePresentation.UNITE_CONDITIONNEMENT, body.uniteConditionnement)
        assertTrue(body.voie.isNotEmpty())
    }

    @Test
    fun `GET presentations by CIP returns 204 NoContent for non-existent CIP`() = testApplication {
        // Préparation
        val client = configureRealServerAndGetClient()

        // Action
        val response = client.get("${ApiRoutes.MEDICAMENT_PRESENTATIONS}/cip/${TestFixtures.CIP_INEXISTANT}")

        // Vérification - L'API retourne 204 NoContent pour les présentations inexistantes
        assertEquals(HttpStatusCode.NoContent, response.status)
    }

    // ─────────────────────────────────────────────────────────────
    // GET /medicament/presentations/cis/{cis}
    // ─────────────────────────────────────────────────────────────

    @Test
    fun `GET presentations by CIS returns multiple presentations`() = testApplication {
        // Préparation
        val client = configureRealServerAndGetClient()
        val cis = TestFixtures.MethotrexateMultiVoies.CIS

        // Action
        val response = client.get("${ApiRoutes.MEDICAMENT_PRESENTATIONS}/cis/$cis")

        // Vérification
        assertEquals(HttpStatusCode.OK, response.status)
        val body = response.body<List<MedicamentPresDetail>>()
        assertEquals(TestFixtures.MethotrexateMultiVoies.NB_PRESENTATIONS, body.size)
        assertTrue(body.all { it.cis == cis })
    }

    // ─────────────────────────────────────────────────────────────
    // GET /medicament/substances/resume/cis/{cis}
    // ─────────────────────────────────────────────────────────────

    @Test
    fun `GET substances resume by CIS returns substances from real database`() = testApplication {
        // Préparation
        val client = configureRealServerAndGetClient()
        val cis = TestFixtures.OxomemazineActive.CIS

        // Action
        val response = client.get("${ApiRoutes.MEDICAMENT_SUBSTANCES}/resume/cis/$cis")

        // Vérification
        assertEquals(HttpStatusCode.OK, response.status)
        val body = response.body<List<MedicamentSubstanceResume>>()
        assertTrue(body.isNotEmpty())
        assertTrue(body.any { it.codeSubstance == TestFixtures.SubstanceOxomemazine.CODE })
    }

    @Test
    fun `GET substances resume returns many substances for multi-substance specialty`() = testApplication {
        // Préparation
        val client = configureRealServerAndGetClient()
        val cis = TestFixtures.Np100MultiSubstances.CIS

        // Action
        val response = client.get("${ApiRoutes.MEDICAMENT_SUBSTANCES}/resume/cis/$cis")

        // Vérification
        assertEquals(HttpStatusCode.OK, response.status)
        val body = response.body<List<MedicamentSubstanceResume>>()
        assertTrue(body.size >= TestFixtures.Np100MultiSubstances.NB_SUBSTANCES_MIN)
    }

    // ─────────────────────────────────────────────────────────────
    // GET /medicament/specialites/status/latest/{cis}
    // ─────────────────────────────────────────────────────────────

    @Test
    fun `GET specialites status latest returns most recent event`() = testApplication {
        // Préparation
        val client = configureRealServerAndGetClient()
        val cis = TestFixtures.EvenementCisActonel.CIS

        // Action
        val response = client.get("${ApiRoutes.MEDICAMENT_SPECIALITES}/status/latest/$cis")

        // Vérification
        assertEquals(HttpStatusCode.OK, response.status)
        val body = response.body<MedicamentSpeStatus>()
        assertEquals(TestFixtures.EvenementCisActonel.TYPE_EVENEMENT_RECENT, body.typeEvenement)
    }

    @Test
    fun `GET specialites status all returns all events`() = testApplication {
        // Préparation
        val client = configureRealServerAndGetClient()
        val cis = TestFixtures.EvenementCisActonel.CIS

        // Action
        val response = client.get("${ApiRoutes.MEDICAMENT_SPECIALITES}/status/all/$cis")

        // Vérification
        assertEquals(HttpStatusCode.OK, response.status)
        val body = response.body<List<MedicamentSpeStatus>>()
        assertEquals(TestFixtures.EvenementCisActonel.NB_EVENEMENTS, body.size)
    }

    // ─────────────────────────────────────────────────────────────
    // GET /medicament/specialites/status/marketed/{cis}
    // ─────────────────────────────────────────────────────────────

    @Test
    fun `GET specialites marketed returns true for authorized medication`() = testApplication {
        // Préparation
        val client = configureRealServerAndGetClient()
        val cis = TestFixtures.CIS_MARKETED

        // Action
        val response = client.get("${ApiRoutes.MEDICAMENT_SPECIALITES}/status/marketed/$cis")

        // Vérification
        assertEquals(HttpStatusCode.OK, response.status)
        val body = response.body<Boolean>()
        assertTrue(body)
    }

    @Test
    fun `GET specialites marketed returns false for archived medication`() = testApplication {
        // Préparation
        val client = configureRealServerAndGetClient()
        val cis = TestFixtures.CIS_NOT_MARKETED

        // Action
        val response = client.get("${ApiRoutes.MEDICAMENT_SPECIALITES}/status/marketed/$cis")

        // Vérification
        assertEquals(HttpStatusCode.OK, response.status)
        val body = response.body<Boolean>()
        assertFalse(body)
    }

    // ─────────────────────────────────────────────────────────────
    // GET /medicament/presentations/status/*
    // ─────────────────────────────────────────────────────────────

    @Test
    fun `GET presentations status marketed returns correct value`() = testApplication {
        // Préparation
        val client = configureRealServerAndGetClient()

        // Action - Tester la présentation commercialisée
        val responseMarketed = client.get("${ApiRoutes.MEDICAMENT_PRESENTATIONS}/status/marketed/${TestFixtures.CIP_MARKETED}")
        val responseNotMarketed = client.get("${ApiRoutes.MEDICAMENT_PRESENTATIONS}/status/marketed/${TestFixtures.CIP_NOT_MARKETED}")

        // Vérification
        assertEquals(HttpStatusCode.OK, responseMarketed.status)
        assertTrue(responseMarketed.body<Boolean>())

        assertEquals(HttpStatusCode.OK, responseNotMarketed.status)
        assertFalse(responseNotMarketed.body<Boolean>())
    }

    @Test
    fun `GET presentations status latest returns event`() = testApplication {
        // Préparation
        val client = configureRealServerAndGetClient()
        val cip13 = TestFixtures.EvenementCipOxomemazine.CIP13

        // Action
        val response = client.get("${ApiRoutes.MEDICAMENT_PRESENTATIONS}/status/latest/$cip13")

        // Vérification
        assertEquals(HttpStatusCode.OK, response.status)
        val body = response.body<MedicamentPresStatus>()
        assertEquals(TestFixtures.EvenementCipOxomemazine.TYPE_EVENEMENT, body.typeEvenement)
    }

    // ─────────────────────────────────────────────────────────────
    // GET /medicament/presentations?libelle=...
    // ─────────────────────────────────────────────────────────────

    @Test
    fun `GET presentations search returns results with voies`() = testApplication {
        // Préparation
        val client = configureRealServerAndGetClient()

        // Action
        val response = client.get("${ApiRoutes.MEDICAMENT_PRESENTATIONS}?libelle=OXOMEMAZINE&limit=5")

        // Vérification
        assertEquals(HttpStatusCode.OK, response.status)
        val body = response.body<List<MedicamentPresDetail>>()
        assertTrue(body.isNotEmpty())
        assertTrue(body.all { it.label.contains("OXOMEMAZINE", ignoreCase = true) })
        assertTrue(body.all { it.voie.isNotEmpty() }, "All presentations should have voies")
    }

    @Test
    fun `GET presentations search returns 204 for no matches`() = testApplication {
        // Préparation
        val client = configureRealServerAndGetClient()

        // Action
        val response = client.get("${ApiRoutes.MEDICAMENT_PRESENTATIONS}?libelle=${TestFixtures.SEARCH_TERM_NO_RESULTS}&limit=10")

        // Vérification
        assertEquals(HttpStatusCode.NoContent, response.status)
    }

    @Test
    fun `GET presentations search returns 400 when libelle missing`() = testApplication {
        // Préparation
        val client = configureRealServerAndGetClient()

        // Action
        val response = client.get("${ApiRoutes.MEDICAMENT_PRESENTATIONS}?limit=10")

        // Vérification
        assertEquals(HttpStatusCode.BadRequest, response.status)
    }

    // ─────────────────────────────────────────────────────────────
    // GET /medicament/presentations/status/all/{cip13}
    // ─────────────────────────────────────────────────────────────

    @Test
    fun `GET presentations status all returns all events`() = testApplication {
        // Préparation
        val client = configureRealServerAndGetClient()
        val cip13 = TestFixtures.CosmogenMultiEvents.CIP13

        // Action
        val response = client.get("${ApiRoutes.MEDICAMENT_PRESENTATIONS}/status/all/$cip13")

        // Vérification
        assertEquals(HttpStatusCode.OK, response.status)
        val body = response.body<List<MedicamentPresStatus>>()
        assertEquals(TestFixtures.CosmogenMultiEvents.NB_EVENEMENTS, body.size)
    }

    @Test
    fun `GET presentations status all returns 204 for CIP with no events`() = testApplication {
        // Préparation
        val client = configureRealServerAndGetClient()

        // Action
        val response = client.get("${ApiRoutes.MEDICAMENT_PRESENTATIONS}/status/all/${TestFixtures.CIP_INEXISTANT}")

        // Vérification
        assertEquals(HttpStatusCode.NoContent, response.status)
    }

    // ─────────────────────────────────────────────────────────────
    // GET /medicament/substances/resume?cis=...&codeSubstance=...
    // ─────────────────────────────────────────────────────────────

    @Test
    fun `GET substances resume returns substance for valid params`() = testApplication {
        // Préparation
        val client = configureRealServerAndGetClient()
        val cis = TestFixtures.OxomemazineActive.CIS
        val codeSubstance = TestFixtures.SubstanceOxomemazine.CODE

        // Action
        val response = client.get("${ApiRoutes.MEDICAMENT_SUBSTANCES}/resume?cis=$cis&codeSubstance=$codeSubstance")

        // Vérification
        assertEquals(HttpStatusCode.OK, response.status)
        val body = response.body<MedicamentSubstanceResume>()
        assertEquals(codeSubstance, body.codeSubstance)
        assertTrue(body.libelle.contains("oxomémazine", ignoreCase = true))
    }

    @Test
    fun `GET substances resume returns 204 for non-existent combination`() = testApplication {
        // Préparation
        val client = configureRealServerAndGetClient()
        val cis = TestFixtures.OxomemazineActive.CIS

        // Action
        val response = client.get("${ApiRoutes.MEDICAMENT_SUBSTANCES}/resume?cis=$cis&codeSubstance=${TestFixtures.CODE_SUBSTANCE_INEXISTANT}")

        // Vérification
        assertEquals(HttpStatusCode.NoContent, response.status)
    }

    @Test
    fun `GET substances resume returns 400 when cis missing`() = testApplication {
        // Préparation
        val client = configureRealServerAndGetClient()

        // Action
        val response = client.get("${ApiRoutes.MEDICAMENT_SUBSTANCES}/resume?codeSubstance=02543")

        // Vérification
        assertEquals(HttpStatusCode.BadRequest, response.status)
    }

    @Test
    fun `GET substances resume returns 400 when codeSubstance missing`() = testApplication {
        // Préparation
        val client = configureRealServerAndGetClient()

        // Action
        val response = client.get("${ApiRoutes.MEDICAMENT_SUBSTANCES}/resume?cis=60035924")

        // Vérification
        assertEquals(HttpStatusCode.BadRequest, response.status)
    }

    @Test
    fun `GET substances resume returns 400 when cis invalid`() = testApplication {
        // Préparation
        val client = configureRealServerAndGetClient()

        // Action
        val response = client.get("${ApiRoutes.MEDICAMENT_SUBSTANCES}/resume?cis=invalid&codeSubstance=02543")

        // Vérification
        assertEquals(HttpStatusCode.BadRequest, response.status)
    }

    // ─────────────────────────────────────────────────────────────
    // GET /medicament/substances/detail/cis/{cis}
    // ─────────────────────────────────────────────────────────────

    @Test
    fun `GET substances detail by CIS returns substances with SMS data`() = testApplication {
        // Préparation
        val client = configureRealServerAndGetClient()
        val cis = TestFixtures.OxomemazineActive.CIS

        // Action
        val response = client.get("${ApiRoutes.MEDICAMENT_SUBSTANCES}/detail/cis/$cis")

        // Vérification
        assertEquals(HttpStatusCode.OK, response.status)
        val body = response.body<List<MedicamentSubstanceDetail>>()
        assertTrue(body.isNotEmpty())
        val oxomemazine = body.find { it.nomSubstance.contains("oxomémazine", ignoreCase = true) }
        assertNotNull(oxomemazine)
        assertEquals(TestFixtures.SubstanceDetailOxomemazine.TYPE_SUBSTANCE, oxomemazine.typeSubstance)
    }

    @Test
    fun `GET substances detail by CIS returns many for multi-substance specialty`() = testApplication {
        // Préparation
        val client = configureRealServerAndGetClient()
        val cis = TestFixtures.Np100MultiSubstances.CIS

        // Action
        val response = client.get("${ApiRoutes.MEDICAMENT_SUBSTANCES}/detail/cis/$cis")

        // Vérification
        assertEquals(HttpStatusCode.OK, response.status)
        val body = response.body<List<MedicamentSubstanceDetail>>()
        assertTrue(body.size >= TestFixtures.Np100MultiSubstances.NB_SUBSTANCES_MIN)
    }

    @Test
    fun `GET substances detail by CIS returns 204 for non-existent CIS`() = testApplication {
        // Préparation
        val client = configureRealServerAndGetClient()

        // Action
        val response = client.get("${ApiRoutes.MEDICAMENT_SUBSTANCES}/detail/cis/${TestFixtures.CIS_INEXISTANT}")

        // Vérification
        assertEquals(HttpStatusCode.NoContent, response.status)
    }

    @Test
    fun `GET substances detail by CIS returns 400 for invalid CIS`() = testApplication {
        // Préparation
        val client = configureRealServerAndGetClient()

        // Action
        val response = client.get("${ApiRoutes.MEDICAMENT_SUBSTANCES}/detail/cis/invalid")

        // Vérification
        assertEquals(HttpStatusCode.BadRequest, response.status)
    }

    // ─────────────────────────────────────────────────────────────
    // GET /medicament/substances/detail?cis=...&codeSubstance=...
    // ─────────────────────────────────────────────────────────────

    @Test
    fun `GET substances detail returns detail for valid params`() = testApplication {
        // Préparation
        val client = configureRealServerAndGetClient()
        val cis = TestFixtures.OxomemazineActive.CIS
        val codeSubstance = TestFixtures.SubstanceOxomemazine.CODE

        // Action
        val response = client.get("${ApiRoutes.MEDICAMENT_SUBSTANCES}/detail?cis=$cis&codeSubstance=$codeSubstance")

        // Vérification
        assertEquals(HttpStatusCode.OK, response.status)
        val body = response.body<MedicamentSubstanceDetail>()
        assertTrue(body.nomSubstance.contains("oxomémazine", ignoreCase = true))
        assertEquals(TestFixtures.SubstanceDetailOxomemazine.FORMULE_MOLECULAIRE, body.formuleMoleculaire)
    }

    @Test
    fun `GET substances detail returns 204 for non-existent combination`() = testApplication {
        // Préparation
        val client = configureRealServerAndGetClient()
        val cis = TestFixtures.OxomemazineActive.CIS

        // Action
        val response = client.get("${ApiRoutes.MEDICAMENT_SUBSTANCES}/detail?cis=$cis&codeSubstance=${TestFixtures.CODE_SUBSTANCE_INEXISTANT}")

        // Vérification
        assertEquals(HttpStatusCode.NoContent, response.status)
    }

    @Test
    fun `GET substances detail returns 400 when cis missing`() = testApplication {
        // Préparation
        val client = configureRealServerAndGetClient()

        // Action
        val response = client.get("${ApiRoutes.MEDICAMENT_SUBSTANCES}/detail?codeSubstance=02543")

        // Vérification
        assertEquals(HttpStatusCode.BadRequest, response.status)
    }

    @Test
    fun `GET substances detail returns 400 when codeSubstance missing`() = testApplication {
        // Préparation
        val client = configureRealServerAndGetClient()

        // Action
        val response = client.get("${ApiRoutes.MEDICAMENT_SUBSTANCES}/detail?cis=60035924")

        // Vérification
        assertEquals(HttpStatusCode.BadRequest, response.status)
    }

    @Test
    fun `GET substances detail returns 400 when cis invalid`() = testApplication {
        // Préparation
        val client = configureRealServerAndGetClient()

        // Action
        val response = client.get("${ApiRoutes.MEDICAMENT_SUBSTANCES}/detail?cis=invalid&codeSubstance=02543")

        // Vérification
        assertEquals(HttpStatusCode.BadRequest, response.status)
    }

    // ─────────────────────────────────────────────────────────────
    // Endpoints existants - Tests 400/204 manquants
    // ─────────────────────────────────────────────────────────────

    @Test
    fun `GET specialites completion returns 400 when libelle missing`() = testApplication {
        // Préparation
        val client = configureRealServerAndGetClient()

        // Action
        val response = client.get("${ApiRoutes.MEDICAMENT_SPECIALITES}/completion?limit=5")

        // Vérification
        assertEquals(HttpStatusCode.BadRequest, response.status)
    }

    @Test
    fun `GET specialites status latest returns 400 for invalid CIS`() = testApplication {
        // Préparation
        val client = configureRealServerAndGetClient()

        // Action
        val response = client.get("${ApiRoutes.MEDICAMENT_SPECIALITES}/status/latest/invalid")

        // Vérification
        assertEquals(HttpStatusCode.BadRequest, response.status)
    }

    @Test
    fun `GET specialites status latest returns 204 for non-existent CIS`() = testApplication {
        // Préparation
        val client = configureRealServerAndGetClient()

        // Action
        val response = client.get("${ApiRoutes.MEDICAMENT_SPECIALITES}/status/latest/${TestFixtures.CIS_INEXISTANT}")

        // Vérification
        assertEquals(HttpStatusCode.NoContent, response.status)
    }

    @Test
    fun `GET specialites status all returns 400 for invalid CIS`() = testApplication {
        // Préparation
        val client = configureRealServerAndGetClient()

        // Action
        val response = client.get("${ApiRoutes.MEDICAMENT_SPECIALITES}/status/all/invalid")

        // Vérification
        assertEquals(HttpStatusCode.BadRequest, response.status)
    }

    @Test
    fun `GET specialites status all returns 204 for non-existent CIS`() = testApplication {
        // Préparation
        val client = configureRealServerAndGetClient()

        // Action
        val response = client.get("${ApiRoutes.MEDICAMENT_SPECIALITES}/status/all/${TestFixtures.CIS_INEXISTANT}")

        // Vérification
        assertEquals(HttpStatusCode.NoContent, response.status)
    }

    @Test
    fun `GET specialites marketed returns 400 for invalid CIS`() = testApplication {
        // Préparation
        val client = configureRealServerAndGetClient()

        // Action
        val response = client.get("${ApiRoutes.MEDICAMENT_SPECIALITES}/status/marketed/invalid")

        // Vérification
        assertEquals(HttpStatusCode.BadRequest, response.status)
    }

    @Test
    fun `GET presentations by CIS returns 400 for invalid CIS`() = testApplication {
        // Préparation
        val client = configureRealServerAndGetClient()

        // Action
        val response = client.get("${ApiRoutes.MEDICAMENT_PRESENTATIONS}/cis/invalid")

        // Vérification
        assertEquals(HttpStatusCode.BadRequest, response.status)
    }

    @Test
    fun `GET presentations status latest returns 204 for non-existent CIP`() = testApplication {
        // Préparation
        val client = configureRealServerAndGetClient()

        // Action
        val response = client.get("${ApiRoutes.MEDICAMENT_PRESENTATIONS}/status/latest/${TestFixtures.CIP_INEXISTANT}")

        // Vérification
        assertEquals(HttpStatusCode.NoContent, response.status)
    }

    @Test
    fun `GET substances resume by CIS returns 400 for invalid CIS`() = testApplication {
        // Préparation
        val client = configureRealServerAndGetClient()

        // Action
        val response = client.get("${ApiRoutes.MEDICAMENT_SUBSTANCES}/resume/cis/invalid")

        // Vérification
        assertEquals(HttpStatusCode.BadRequest, response.status)
    }
}
