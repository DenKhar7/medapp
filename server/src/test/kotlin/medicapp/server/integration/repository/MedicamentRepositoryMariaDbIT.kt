package medicapp.server.integration.repository

import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import medicapp.server.infrastructure.db.requests.repository.MedicamentRepositoryMariaDb
import medicapp.server.integration.config.IntegrationTestExtension
import medicapp.server.integration.fixtures.TestFixtures
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Tests d'intégration pour MedicamentRepositoryMariaDb.
 *
 * Ces tests valident :
 * - Les requêtes SQL s'exécutent correctement contre une vraie MariaDB
 * - Les jointures retournent les données attendues depuis plusieurs tables
 * - La logique de regroupement gère les relations un-vers-plusieurs
 * - Les cas limites (nulls, résultats vides, caractères spéciaux)
 *
 * Les données de test sont chargées depuis les fichiers CSV définis dans le projet basededonnee.
 */
@ExtendWith(IntegrationTestExtension::class)
class MedicamentRepositoryMariaDbIT {

    private lateinit var repository: MedicamentRepositoryMariaDb

    @BeforeEach
    fun setup() {
        repository = MedicamentRepositoryMariaDb()
    }

    // ─────────────────────────────────────────────────────────────
    // Tests getSpeResumeByCis
    // ─────────────────────────────────────────────────────────────

    @Test
    fun `getSpeResumeByCis returns specialty with voies for active CIS`() = runTest {
        // Préparation - CIS 60035924 is OXOMEMAZINE (active, voie orale)
        val cis = TestFixtures.OxomemazineActive.CIS

        // Action
        val result = repository.getSpeResumeByCis(cis)

        // Vérification
        assertNotNull(result, "Specialty should exist")
        assertEquals(cis, result.cis)
        assertTrue(result.nom.contains("OXOMEMAZINE"), "Name should contain OXOMEMAZINE")
        assertEquals(TestFixtures.OxomemazineActive.CODE_ATC, result.codeAtc)
        assertEquals(TestFixtures.OxomemazineActive.LIBELLE_ATC, result.libelleAtc)
        assertEquals(TestFixtures.OxomemazineActive.ORGANISATION, result.nomOrganisation)
        assertTrue(result.voies.isNotEmpty(), "Voies should not be empty")
        assertTrue(result.voies.contains("orale"), "Should have voie orale")
    }

    @Test
    fun `getSpeResumeByCis returns specialty for archived CIS`() = runTest {
        // Préparation - CIS 60262879 is ACTONEL (archived)
        val cis = TestFixtures.ActonelArchive.CIS

        // Action
        val result = repository.getSpeResumeByCis(cis)

        // Vérification
        assertNotNull(result, "Archived specialty should still be found")
        assertEquals(cis, result.cis)
        assertTrue(result.nom.contains("ACTONEL"), "Name should contain ACTONEL")
        assertEquals(TestFixtures.ActonelArchive.CODE_ATC, result.codeAtc)
    }

    @Test
    fun `getSpeResumeByCis returns specialty with multiple voies`() = runTest {
        // Préparation - CIS 67217445 is METHOTREXATE with multiple voies
        val cis = TestFixtures.MethotrexateMultiVoies.CIS

        // Action
        val result = repository.getSpeResumeByCis(cis)

        // Vérification
        assertNotNull(result)
        assertTrue(result.voies.size >= 2, "Should have multiple voies, got: ${result.voies}")
    }

    @Test
    fun `getSpeResumeByCis returns null for non-existent CIS`() = runTest {
        // Préparation
        val nonExistentCis = TestFixtures.CIS_INEXISTANT

        // Action
        val result = repository.getSpeResumeByCis(nonExistentCis)

        // Vérification
        assertNull(result, "Should return null for non-existent CIS")
    }

    // ─────────────────────────────────────────────────────────────
    // Tests searchSpeResumeByLibelle
    // ─────────────────────────────────────────────────────────────

    @Test
    fun `searchSpeResumeByLibelle finds partial matches`() = runTest {
        // Préparation
        val searchTerm = TestFixtures.SEARCH_TERM_WITH_RESULTS

        // Action
        val results = repository.searchSpeResumeByLibelle(searchTerm, limit = 10)

        // Vérification
        assertTrue(results.isNotEmpty(), "Should find at least one result for '$searchTerm'")
        assertTrue(
            results.all { it.nom.contains(searchTerm, ignoreCase = true) },
            "All results should contain the search term"
        )
    }

    @Test
    fun `searchSpeResumeByLibelle returns empty list for no matches`() = runTest {
        // Préparation
        val searchTerm = TestFixtures.SEARCH_TERM_NO_RESULTS

        // Action
        val results = repository.searchSpeResumeByLibelle(searchTerm, limit = 10)

        // Vérification
        assertTrue(results.isEmpty(), "Should return empty list for non-matching search")
    }

    @Test
    fun `searchSpeResumeByLibelle respects limit`() = runTest {
        // Préparation
        val searchTerm = TestFixtures.SEARCH_TERM_MULTIPLE_RESULTS
        val limit = 2

        // Action
        val results = repository.searchSpeResumeByLibelle(searchTerm, limit)

        // Vérification
        assertTrue(results.size <= limit, "Results should not exceed limit of $limit, got ${results.size}")
    }

    // ─────────────────────────────────────────────────────────────
    // Tests getLibelleCompletion
    // ─────────────────────────────────────────────────────────────

    @Test
    fun `getLibelleCompletion returns suggestions for partial text`() = runTest {
        // Préparation
        val searchExtract = "OXOME"

        // Action
        val results = repository.getLibelleCompletion(searchExtract, limit = 5)

        // Vérification
        assertTrue(results.isNotEmpty(), "Should return completion suggestions")
        assertTrue(results.all { it.libelle.contains(searchExtract, ignoreCase = true) })
    }

    // ─────────────────────────────────────────────────────────────
    // Tests getPresDetailByCip
    // ─────────────────────────────────────────────────────────────

    @Test
    fun `getPresDetailByCip returns presentation with all joined data`() = runTest {
        // Préparation
        val cip13 = TestFixtures.OxomemazinePresentation.CIP13

        // Action
        val result = repository.getPresDetailByCip(cip13)

        // Vérification
        assertNotNull(result, "Presentation should exist")
        assertEquals(cip13, result.cip13)
        assertEquals(TestFixtures.OxomemazinePresentation.CIS, result.cis)
        assertTrue(result.label.contains("OXOMEMAZINE"), "Label should contain OXOMEMAZINE")
        assertNotNull(result.quantiteConditionnement)
        assertEquals(TestFixtures.OxomemazinePresentation.QUANTITE_CONDITIONNEMENT, result.quantiteConditionnement)
        assertEquals(TestFixtures.OxomemazinePresentation.UNITE_CONDITIONNEMENT, result.uniteConditionnement)
        assertEquals(TestFixtures.OxomemazinePresentation.NB_UNITE_DISP, result.dosesParBoite)
        assertNotNull(result.typeDispositif)
        assertTrue(result.voie.isNotEmpty(), "Voies should not be empty")
    }

    @Test
    fun `getPresDetailByCip returns null for non-existent CIP`() = runTest {
        // Préparation
        val nonExistentCip = TestFixtures.CIP_INEXISTANT

        // Action
        val result = repository.getPresDetailByCip(nonExistentCip)

        // Vérification
        assertNull(result, "Should return null for non-existent CIP")
    }

    // ─────────────────────────────────────────────────────────────
    // Tests getAllPresDetailByCis
    // ─────────────────────────────────────────────────────────────

    @Test
    fun `getAllPresDetailByCis returns all presentations for specialty`() = runTest {
        // Préparation - CIS 67217445 (METHOTREXATE) has 2 presentations
        val cis = TestFixtures.MethotrexateMultiVoies.CIS

        // Action
        val results = repository.getAllPresDetailByCis(cis)

        // Vérification
        assertEquals(
            TestFixtures.MethotrexateMultiVoies.NB_PRESENTATIONS,
            results.size,
            "Should return ${TestFixtures.MethotrexateMultiVoies.NB_PRESENTATIONS} presentations"
        )
        assertTrue(results.all { it.cis == cis }, "All presentations should belong to the same CIS")
    }

    @Test
    fun `getAllPresDetailByCis returns empty list for CIS with no presentations`() = runTest {
        // Préparation
        val nonExistentCis = TestFixtures.CIS_INEXISTANT

        // Action
        val results = repository.getAllPresDetailByCis(nonExistentCis)

        // Vérification
        assertTrue(results.isEmpty(), "Should return empty list for CIS with no presentations")
    }

    // ─────────────────────────────────────────────────────────────
    // Tests getSubstancesResumeByCis
    // ─────────────────────────────────────────────────────────────

    @Test
    fun `getSubstancesResumeByCis returns substances for specialty`() = runTest {
        // Préparation - CIS 60035924 (OXOMEMAZINE) has oxomémazine substance
        val cis = TestFixtures.OxomemazineActive.CIS

        // Action
        val results = repository.getSubstancesResumeByCis(cis)

        // Vérification
        assertTrue(results.isNotEmpty(), "Should return at least one substance")
        assertTrue(
            results.any { it.codeSubstance == TestFixtures.SubstanceOxomemazine.CODE },
            "Should contain oxomémazine substance"
        )
    }

    @Test
    fun `getSubstancesResumeByCis returns multiple substances for multi-substance specialty`() = runTest {
        // Préparation - CIS 60208447 (NP100) has many substances
        val cis = TestFixtures.Np100MultiSubstances.CIS

        // Action
        val results = repository.getSubstancesResumeByCis(cis)

        // Vérification
        assertTrue(
            results.size >= TestFixtures.Np100MultiSubstances.NB_SUBSTANCES_MIN,
            "NP100 should have at least ${TestFixtures.Np100MultiSubstances.NB_SUBSTANCES_MIN} substances, got ${results.size}"
        )
    }

    // ─────────────────────────────────────────────────────────────
    // Tests getLatestEventByCis / getAllEventsByCis
    // ─────────────────────────────────────────────────────────────

    @Test
    fun `getLatestEventByCis returns most recent event`() = runTest {
        // Préparation - CIS 60262879 has authorization then archival
        val cis = TestFixtures.EvenementCisActonel.CIS

        // Action
        val result = repository.getLatestEventByCis(cis)

        // Vérification
        assertNotNull(result, "Should return latest event")
        assertEquals(
            TestFixtures.EvenementCisActonel.TYPE_EVENEMENT_RECENT,
            result.typeEvenement,
            "Latest event should be archival"
        )
        assertEquals(LocalDate.parse(TestFixtures.EvenementCisActonel.DATE_EFFET), result.dateEffet)
    }

    @Test
    fun `getAllEventsByCis returns all events for specialty`() = runTest {
        // Préparation
        val cis = TestFixtures.EvenementCisActonel.CIS

        // Action
        val results = repository.getAllEventsByCis(cis)

        // Vérification
        assertEquals(
            TestFixtures.EvenementCisActonel.NB_EVENEMENTS,
            results.size,
            "Should return ${TestFixtures.EvenementCisActonel.NB_EVENEMENTS} events"
        )
    }

    @Test
    fun `getLatestEventByCis returns null for CIS with no events`() = runTest {
        // Préparation
        val nonExistentCis = TestFixtures.CIS_INEXISTANT

        // Action
        val result = repository.getLatestEventByCis(nonExistentCis)

        // Vérification
        assertNull(result, "Should return null for CIS with no events")
    }

    // ─────────────────────────────────────────────────────────────
    // Tests isSpeMarketed / isPresMarketed
    // ─────────────────────────────────────────────────────────────

    @Test
    fun `isSpeMarketed returns true for authorized medication`() = runTest {
        // Préparation - CIS 60035924 has Autorisation event
        val cis = TestFixtures.CIS_MARKETED

        // Action
        val result = repository.isSpeMarketed(cis)

        // Vérification
        assertTrue(result, "Authorized medication should be marketed")
    }

    @Test
    fun `isSpeMarketed returns false for archived medication`() = runTest {
        // Préparation - CIS 60262879 is archived
        val cis = TestFixtures.CIS_NOT_MARKETED

        // Action
        val result = repository.isSpeMarketed(cis)

        // Vérification
        assertFalse(result, "Archived medication should not be marketed")
    }

    @Test
    fun `isPresMarketed returns true for commercialized presentation`() = runTest {
        // Préparation
        val cip13 = TestFixtures.CIP_MARKETED

        // Action
        val result = repository.isPresMarketed(cip13)

        // Vérification
        assertTrue(result, "Commercialized presentation should be marketed")
    }

    @Test
    fun `isPresMarketed returns false for discontinued presentation`() = runTest {
        // Préparation - CIP 3400949020539 has "Arrêt de Commercialisation"
        val cip13 = TestFixtures.CIP_NOT_MARKETED

        // Action
        val result = repository.isPresMarketed(cip13)

        // Vérification
        assertFalse(result, "Discontinued presentation should not be marketed")
    }

    // ─────────────────────────────────────────────────────────────
    // Tests getLatestEventByCip / getAllEventsByCip
    // ─────────────────────────────────────────────────────────────

    @Test
    fun `getLatestEventByCip returns most recent event`() = runTest {
        // Préparation
        val cip13 = TestFixtures.EvenementCipOxomemazine.CIP13

        // Action
        val result = repository.getLatestEventByCip(cip13)

        // Vérification
        assertNotNull(result)
        assertEquals(TestFixtures.EvenementCipOxomemazine.TYPE_EVENEMENT, result.typeEvenement)
    }

    @Test
    fun `getAllEventsByCip returns all events for presentation`() = runTest {
        // Préparation
        val cip13 = TestFixtures.EvenementCipOxomemazine.CIP13

        // Action
        val results = repository.getAllEventsByCip(cip13)

        // Vérification
        assertTrue(results.isNotEmpty(), "Should return at least one event")
    }

    // ─────────────────────────────────────────────────────────────
    // Tests searchPresDetailByLibelle
    // ─────────────────────────────────────────────────────────────

    @Test
    fun `searchPresDetailByLibelle finds partial matches`() = runTest {
        // Préparation
        val searchTerm = TestFixtures.SEARCH_TERM_WITH_RESULTS

        // Action
        val results = repository.searchPresDetailByLibelle(searchTerm, limit = 10)

        // Vérification
        assertTrue(results.isNotEmpty(), "Should find at least one result for '$searchTerm'")
        assertTrue(
            results.all { it.label.contains(searchTerm, ignoreCase = true) },
            "All results should contain the search term"
        )
        assertTrue(results.all { it.voie.isNotEmpty() }, "All results should have voies joined")
    }

    @Test
    fun `searchPresDetailByLibelle returns empty list for no matches`() = runTest {
        // Préparation
        val searchTerm = TestFixtures.SEARCH_TERM_NO_RESULTS

        // Action
        val results = repository.searchPresDetailByLibelle(searchTerm, limit = 10)

        // Vérification
        assertTrue(results.isEmpty(), "Should return empty list for non-matching search")
    }

    @Test
    fun `searchPresDetailByLibelle respects limit`() = runTest {
        // Préparation
        val searchTerm = TestFixtures.SEARCH_TERM_MULTIPLE_RESULTS
        val limit = 2

        // Action
        val results = repository.searchPresDetailByLibelle(searchTerm, limit)

        // Vérification
        assertTrue(results.size <= limit, "Results should not exceed limit of $limit, got ${results.size}")
    }

    // ─────────────────────────────────────────────────────────────
    // Tests getSubstanceResumeByCisAndCode
    // ─────────────────────────────────────────────────────────────

    @Test
    fun `getSubstanceResumeByCisAndCode returns substance for valid combination`() = runTest {
        // Préparation - CIS 60035924 (OXOMEMAZINE) + code 02543
        val cis = TestFixtures.OxomemazineActive.CIS
        val codeSubstance = TestFixtures.SubstanceOxomemazine.CODE

        // Action
        val result = repository.getSubstanceResumeByCisAndCode(cis, codeSubstance)

        // Vérification
        assertNotNull(result, "Substance should exist for valid CIS + code combination")
        assertEquals(codeSubstance, result.codeSubstance)
        assertTrue(result.libelle.contains("oxomémazine", ignoreCase = true))
    }

    @Test
    fun `getSubstanceResumeByCisAndCode returns null for non-existent code`() = runTest {
        // Préparation - CIS valide mais code inexistant
        val cis = TestFixtures.OxomemazineActive.CIS
        val codeSubstance = TestFixtures.CODE_SUBSTANCE_INEXISTANT

        // Action
        val result = repository.getSubstanceResumeByCisAndCode(cis, codeSubstance)

        // Vérification
        assertNull(result, "Should return null for non-existent substance code")
    }

    @Test
    fun `getSubstanceResumeByCisAndCode returns null for non-existent CIS`() = runTest {
        // Préparation - CIS inexistant
        val cis = TestFixtures.CIS_INEXISTANT
        val codeSubstance = TestFixtures.SubstanceOxomemazine.CODE

        // Action
        val result = repository.getSubstanceResumeByCisAndCode(cis, codeSubstance)

        // Vérification
        assertNull(result, "Should return null for non-existent CIS")
    }

    // ─────────────────────────────────────────────────────────────
    // Tests getSubstancesDetailByCis
    // ─────────────────────────────────────────────────────────────

    @Test
    fun `getSubstancesDetailByCis returns substances with SMS data`() = runTest {
        // Préparation - CIS 60035924 (OXOMEMAZINE)
        val cis = TestFixtures.OxomemazineActive.CIS

        // Action
        val results = repository.getSubstancesDetailByCis(cis)

        // Vérification
        assertTrue(results.isNotEmpty(), "Should return at least one substance")
        val oxomemazine = results.find { it.nomSubstance.contains("oxomémazine", ignoreCase = true) }
        assertNotNull(oxomemazine, "Should contain oxomémazine substance")
        assertEquals(TestFixtures.SubstanceDetailOxomemazine.TYPE_SUBSTANCE, oxomemazine.typeSubstance)
        assertEquals(TestFixtures.SubstanceDetailOxomemazine.FORMULE_MOLECULAIRE, oxomemazine.formuleMoleculaire)
    }

    @Test
    fun `getSubstancesDetailByCis returns multiple for multi-substance specialty`() = runTest {
        // Préparation - CIS 60208447 (NP100) has many substances
        val cis = TestFixtures.Np100MultiSubstances.CIS

        // Action
        val results = repository.getSubstancesDetailByCis(cis)

        // Vérification
        assertTrue(
            results.size >= TestFixtures.Np100MultiSubstances.NB_SUBSTANCES_MIN,
            "NP100 should have at least ${TestFixtures.Np100MultiSubstances.NB_SUBSTANCES_MIN} substances, got ${results.size}"
        )
    }

    @Test
    fun `getSubstancesDetailByCis returns empty for non-existent CIS`() = runTest {
        // Préparation
        val nonExistentCis = TestFixtures.CIS_INEXISTANT

        // Action
        val results = repository.getSubstancesDetailByCis(nonExistentCis)

        // Vérification
        assertTrue(results.isEmpty(), "Should return empty list for non-existent CIS")
    }

    // ─────────────────────────────────────────────────────────────
    // Tests getSubstanceDetailByCisAndCode
    // ─────────────────────────────────────────────────────────────

    @Test
    fun `getSubstanceDetailByCisAndCode returns detail for valid combination`() = runTest {
        // Préparation - CIS 60035924 (OXOMEMAZINE) + code 02543
        val cis = TestFixtures.OxomemazineActive.CIS
        val codeSubstance = TestFixtures.SubstanceOxomemazine.CODE

        // Action
        val result = repository.getSubstanceDetailByCisAndCode(cis, codeSubstance)

        // Vérification
        assertNotNull(result, "Substance detail should exist for valid CIS + code combination")
        assertTrue(result.nomSubstance.contains("oxomémazine", ignoreCase = true))
        assertEquals(TestFixtures.SubstanceDetailOxomemazine.FORMULE_MOLECULAIRE, result.formuleMoleculaire)
        assertEquals(TestFixtures.SubstanceDetailOxomemazine.TYPE_SUBSTANCE, result.typeSubstance)
    }

    @Test
    fun `getSubstanceDetailByCisAndCode returns null for non-existent combination`() = runTest {
        // Préparation - code inexistant
        val cis = TestFixtures.OxomemazineActive.CIS
        val codeSubstance = TestFixtures.CODE_SUBSTANCE_INEXISTANT

        // Action
        val result = repository.getSubstanceDetailByCisAndCode(cis, codeSubstance)

        // Vérification
        assertNull(result, "Should return null for non-existent substance code")
    }

    // ─────────────────────────────────────────────────────────────
    // Tests getAllEventsByCip (additional)
    // ─────────────────────────────────────────────────────────────

    @Test
    fun `getAllEventsByCip returns multiple events for CIP with history`() = runTest {
        // Préparation - CIP 3400956434299 has 3 events
        val cip13 = TestFixtures.CosmogenMultiEvents.CIP13

        // Action
        val results = repository.getAllEventsByCip(cip13)

        // Vérification
        assertEquals(
            TestFixtures.CosmogenMultiEvents.NB_EVENEMENTS,
            results.size,
            "Should return ${TestFixtures.CosmogenMultiEvents.NB_EVENEMENTS} events"
        )
    }
}
