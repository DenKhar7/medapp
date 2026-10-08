package medicapp.server.infrastructure.jobs.util

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.HttpHeaders
import io.ktor.http.isSuccess
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.readRemaining
import java.io.File

object DownloadService {

    // Les ZIP des référentiels pèsent plusieurs dizaines de Mo : le délai par défaut du moteur CIO
    // (quelques secondes pour toute la requête) fait échouer le téléchargement sur une connexion lente.
    private const val CONNECT_TIMEOUT_MS = 30_000L
    private const val INACTIVITY_TIMEOUT_MS = 120_000L // aucune donnée reçue pendant 2 min => échec
    private const val TOTAL_TIMEOUT_MS = 30 * 60_000L // plafond global de 30 min

    /**
     * Fonction utilitaire utilisé pour télécharger le fichier ZIP lorsque la base de donnée
     * doit être mis à jour
     * @param fileUrl : l'URL pour télécharger le fichier ZIP via une requête GET
     * @param targetFile : Le fichier dans lequel on écrit les données téléchargées
     */
    suspend fun download(
        fileUrl: String,
        targetFile : File
    ) {
        val client = HttpClient(CIO) {
            expectSuccess = false
            install(HttpTimeout) {
                connectTimeoutMillis = CONNECT_TIMEOUT_MS
                socketTimeoutMillis = INACTIVITY_TIMEOUT_MS
                requestTimeoutMillis = TOTAL_TIMEOUT_MS
            }
        }

        client.use { client ->
            val response: HttpResponse = client.get(fileUrl) {
                header(HttpHeaders.UserAgent, "ReferentielUpdater/1.0")
            }

            if (!response.status.isSuccess()) {
                error("HTTP ${response.status.value} while downloading $fileUrl")
            }

            targetFile.parentFile?.mkdirs()

            val channel: ByteReadChannel = response.bodyAsChannel()

            // Flux bufferisé : sans lui, chaque octet déclenche un appel système d'écriture.
            targetFile.outputStream().buffered().use { output ->
                while (!channel.isClosedForRead) {
                    val packet = channel.readRemaining(DEFAULT_BUFFER_SIZE.toLong())
                    while (!packet.exhausted()) {
                        val bytes = packet.readByte()
                        output.write(bytes.toInt())
                    }
                }
            }
        }
    }
}
