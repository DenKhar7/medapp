package medicapp.server.infrastructure.jobs.util

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup

class HtmlVersionChecker(
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {

    /**
     * Scrape la version depuis la page HTML fournie.
     *
     * Responsabilité unique :
     * - se connecter à l'URL
     * - parser le HTML
     * - extraire la version recherchée
     *
     * @param url URL de la page web à analyser
     * @return la version trouvée ou null si non détectée
     *
     * @throws org.jsoup.HttpStatusException
     * @throws java.io.IOException
     */
    suspend fun scrapeWebsiteVersion(url: String): String? {
        val doc = withContext(ioDispatcher) {
            Jsoup.connect(url)
                .timeout(10_000)
                .get()
        }


        val tbody = doc
            .selectFirst("body")
            ?.selectFirst("tbody")
            ?: return null

        val rows = tbody.select("tr")

        for (row in rows) {
            val headerText = row.selectFirst("th")?.text().orEmpty()
            if (headerText.startsWith("2.")) {
                return row.selectFirst("td")?.text()?.trim()
            }
        }

        return null
    }

    /**
     * Vérifie si une nouvelle version est disponible
     * en comparant la version actuelle avec celle scrapée.
     *
     * Méthode destinée à être utilisée dans un job.
     *
     * @param url URL de la page web
     * @param currentVersion version actuellement connue
     *
     * @return Pair<hasUpdate, websiteVersion>
     */
    suspend fun checkForUpdate(
        url: String,
        currentVersion: String
    ): Pair<Boolean, String> {

        val websiteVersion = scrapeWebsiteVersion(url)
            ?: return false to ""

        val hasUpdate = websiteVersion != currentVersion

        return hasUpdate to websiteVersion
    }
}
