package iut.butinfo3.app_mobile.unit

import iut.butinfo3.app_mobile.model.api.MedicamentApiClient.Companion.buildApiBaseUrl
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/**
 * Tests unitaires de la construction de l'URL de base de l'API.
 * Le serveur expose ses routes sous /api/v1/medicament : le client doit toujours l'ajouter.
 *
 * Écrits avec JUnit 5 car le module exécute les tests via useJUnitPlatform().
 */
class MedicamentApiClientUrlTest {

    @Test
    fun serverRoot_getsApiPrefix() {
        assertEquals("http://10.0.2.2:4000/api/v1/medicament", buildApiBaseUrl("http://10.0.2.2:4000"))
    }

    @Test
    fun trailingSlash_isIgnored() {
        assertEquals("https://meds.example.com/api/v1/medicament", buildApiBaseUrl("https://meds.example.com/"))
    }

    @Test
    fun surroundingWhitespace_isIgnored() {
        assertEquals("http://localhost:4000/api/v1/medicament", buildApiBaseUrl("  http://localhost:4000  "))
    }

    @Test
    fun alreadyPrefixed_isNotDuplicated() {
        assertEquals(
            "http://10.0.2.2:4000/api/v1/medicament",
            buildApiBaseUrl("http://10.0.2.2:4000/api/v1/medicament")
        )
        assertEquals(
            "http://10.0.2.2:4000/api/v1/medicament",
            buildApiBaseUrl("http://10.0.2.2:4000/api/v1/medicament/")
        )
    }
}
