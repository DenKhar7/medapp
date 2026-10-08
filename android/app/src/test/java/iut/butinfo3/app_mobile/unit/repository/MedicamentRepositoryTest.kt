package iut.butinfo3.app_mobile.unit.repository

import iut.butinfo3.app_mobile.model.api.MedicamentApiClient
import iut.butinfo3.app_mobile.model.dao.MedicamentDao
import iut.butinfo3.app_mobile.model.entity.MedicamentPresDetail
import iut.butinfo3.app_mobile.model.entity.MedicamentSpeResume
import iut.butinfo3.app_mobile.model.repository.MedicamentRepository
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class MedicamentRepositoryTest {

    private lateinit var dao: MedicamentDao
    private lateinit var apiClient: MedicamentApiClient
    private lateinit var repository: MedicamentRepository

    private val samplePresentation = MedicamentPresDetail(
        cip13 = "3400930000001",
        label = "Doliprane 1000mg",
        nomSpecialite = "DOLIPRANE",
        cis = 60001234,
        nomOrganisation = "SANOFI",
        codeAtc = "N02BE01",
        libelleAtc = "Paracétamol",
        voie = listOf("orale"),
        dosesParBoite = 8,
        quantiteConditionnement = 1000.0,
        uniteConditionnement = "mg",
        typeDispositif = null
    )

    private val sampleSpecialite = MedicamentSpeResume(
        cis = 60001234,
        nom = "DOLIPRANE",
        nomOrganisation = "SANOFI",
        codeAtc = "N02BE01",
        libelleAtc = "Paracétamol",
        voies = listOf("orale")
    )

    @BeforeEach
    fun setup() {
        dao = mockk(relaxed = true)
        apiClient = mockk(relaxed = true)
        repository = MedicamentRepository(dao, apiClient)
    }

    @Test
    fun searchByLibelle_cachesPresentationIfAbsentInDb() = runTest {
        coEvery { apiClient.searchPresentations("doliprane", 10) } returns listOf(samplePresentation)
        coEvery { dao.getByCip("3400930000001") } returns null
        coEvery { dao.getSpeByCis(60001234) } returns null
        coEvery { apiClient.getSpecialiteByCis(60001234) } returns sampleSpecialite

        val result = repository.searchByLibelle("doliprane")

        assertEquals(1, result.size)
        coVerify { dao.insert(sampleSpecialite) }
        coVerify { dao.insert(samplePresentation) }
    }

    @Test
    fun searchByLibelle_doesNotCacheIfAlreadyInDb() = runTest {
        coEvery { apiClient.searchPresentations("doliprane", 10) } returns listOf(samplePresentation)
        coEvery { dao.getByCip("3400930000001") } returns samplePresentation

        val result = repository.searchByLibelle("doliprane")

        assertEquals(1, result.size)
        coVerify(exactly = 0) { dao.insert(any<MedicamentPresDetail>()) }
        coVerify(exactly = 0) { dao.insert(any<MedicamentSpeResume>()) }
    }

    @Test
    fun searchByLibelle_fetchesSpecialiteIfAbsent() = runTest {
        coEvery { apiClient.searchPresentations("doliprane", 10) } returns listOf(samplePresentation)
        coEvery { dao.getByCip("3400930000001") } returns null
        coEvery { dao.getSpeByCis(60001234) } returns null
        coEvery { apiClient.getSpecialiteByCis(60001234) } returns sampleSpecialite

        repository.searchByLibelle("doliprane")

        coVerify { apiClient.getSpecialiteByCis(60001234) }
        coVerify { dao.insert(sampleSpecialite) }
    }

    @Test
    fun searchByLibelle_doesNotFetchSpecialiteIfAlreadyInDb() = runTest {
        coEvery { apiClient.searchPresentations("doliprane", 10) } returns listOf(samplePresentation)
        coEvery { dao.getByCip("3400930000001") } returns null
        coEvery { dao.getSpeByCis(60001234) } returns sampleSpecialite

        repository.searchByLibelle("doliprane")

        coVerify(exactly = 0) { apiClient.getSpecialiteByCis(any()) }
        coVerify(exactly = 0) { dao.insert(any<MedicamentSpeResume>()) }
    }

    @Test
    fun searchSpecialites_delegatesToApi() = runTest {
        coEvery { apiClient.searchSpecialites("doliprane", 10) } returns listOf(sampleSpecialite)

        val result = repository.searchSpecialites("doliprane")

        assertEquals(1, result.size)
        assertEquals("DOLIPRANE", result[0].nom)
    }

    @Test
    fun getPresentationsByCis_delegatesToApi() = runTest {
        coEvery { apiClient.getPresentationsByCis(60001234) } returns listOf(samplePresentation)

        val result = repository.getPresentationsByCis(60001234)

        assertEquals(1, result.size)
        coVerify { apiClient.getPresentationsByCis(60001234) }
    }
}
