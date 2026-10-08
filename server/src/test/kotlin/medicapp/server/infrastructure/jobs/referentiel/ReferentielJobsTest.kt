package medicapp.server.infrastructure.jobs.referentiel

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import medicapp.server.infrastructure.db.enums.DataSourceEnum
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class ReferentielJobsTest {

    private val orchestrator = mockk<ReferentielUpdateOrchestrator>()
    private val failures = mutableListOf<Pair<DataSourceEnum, String>>()
    private val jobs = ReferentielJobs(orchestrator) { source, reason -> failures += source to reason }

    @Test
    fun `une mise a jour reussie n enregistre aucun echec`() = runBlocking {
        coEvery { orchestrator.run(any()) } returns Unit

        jobs.updateRuimJob()

        assertTrue(failures.isEmpty())
        coVerify(exactly = 1) { orchestrator.run(match { it.source == DataSourceEnum.RUIM }) }
    }

    @Test
    fun `un echec est enregistre sans faire remonter d exception`() = runBlocking {
        coEvery { orchestrator.run(any()) } throws IllegalStateException("Import RUIM rejeté : table vide")

        jobs.updateRuimJob() // ne doit pas lever : le Timer du serveur doit continuer à planifier le job

        assertEquals(1, failures.size)
        assertEquals(DataSourceEnum.RUIM, failures.single().first)
        assertTrue("table vide" in failures.single().second)
    }

    @Test
    fun `l echec du job SMS est attribue au referentiel SMS`() = runBlocking {
        coEvery { orchestrator.run(any()) } throws RuntimeException("HTTP 503")

        jobs.updateSmsJob()

        assertEquals(DataSourceEnum.SMS, failures.single().first)
    }

    @Test
    fun `l annulation de coroutine n est pas avalee`() {
        coEvery { orchestrator.run(any()) } throws CancellationException("arrêt du serveur")

        assertFailsWith<CancellationException> { runBlocking { jobs.updateRuimJob() } }
        assertTrue(failures.isEmpty())
    }

    @Test
    fun `une erreur lors de l enregistrement de l echec ne casse pas le job`() = runBlocking {
        coEvery { orchestrator.run(any()) } throws RuntimeException("boom")
        val fragile = ReferentielJobs(orchestrator) { _, _ -> error("base indisponible") }

        fragile.updateRuimJob() // ne doit pas lever
    }
}
