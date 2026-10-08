package medicapp.server.infrastructure.jobs

import medicapp.server.infrastructure.db.enums.DataSourceEnum
import medicapp.server.infrastructure.jobs.referentiel.ReferentielContextBuilder
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class JobsConstantsTest {

    @Test
    fun `download url embeds the requested version and terminology`() {
        val url = JobsConstants.downloadUrl("terminologie-sms", "2026-09")

        assertTrue("terminologyId=terminologie-sms" in url)
        assertTrue("version=2026-09" in url)
        assertTrue(url.startsWith("https://smt.esante.gouv.fr/wp-json/ans/terminologies/zip?"))
    }

    @Test
    fun `different versions give different urls`() {
        assertTrue(JobsConstants.ruimDownloadUrl("2026-09") != JobsConstants.ruimDownloadUrl("2026-10"))
    }

    @Test
    fun `ruim context downloads the version it is given`() {
        val context = ReferentielContextBuilder().buildRuimContext()

        assertEquals(DataSourceEnum.RUIM, context.source)
        assertTrue("terminologyId=terminologie-ref_interop_med" in context.downloadUrl("2026-09"))
        assertTrue("version=2026-09" in context.downloadUrl("2026-09"))
        assertTrue("version=2026-10" in context.downloadUrl("2026-10"))
    }

    @Test
    fun `sms context downloads the version it is given`() {
        val context = ReferentielContextBuilder().buildSmsContext()

        assertEquals(DataSourceEnum.SMS, context.source)
        assertTrue("terminologyId=terminologie-sms" in context.downloadUrl("2026-09"))
        assertTrue("version=2026-09" in context.downloadUrl("2026-09"))
    }

    @Test
    fun `no download url is pinned to a hard-coded version`() {
        val urls = listOf(JobsConstants.RUIM_URL, JobsConstants.SMS_URL)
        urls.forEach { assertTrue("version=" !in it, "Page URL must not carry a version: $it") }
    }
}
