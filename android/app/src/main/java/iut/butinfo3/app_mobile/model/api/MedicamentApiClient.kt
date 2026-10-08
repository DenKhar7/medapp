package iut.butinfo3.app_mobile.model.api

import iut.butinfo3.app_mobile.BuildConfig
import iut.butinfo3.app_mobile.model.entity.MedicamentPresDetail
import iut.butinfo3.app_mobile.model.entity.MedicamentSpeResume
import iut.butinfo3.app_mobile.model.entity.MedicamentSubstanceDetail
import iut.butinfo3.app_mobile.model.entity.MedicamentSubstanceResume
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.gson.gson

class MedicamentApiClient {

    // BASE_URL est la racine du serveur (ex : http://10.0.2.2:4000), le préfixe de l'API est ajouté ici.
    private val baseUrl = buildApiBaseUrl(BuildConfig.BASE_URL)

    private val client = sharedClient

    suspend fun getSpecialiteByCis(cis: Int): MedicamentSpeResume {
        return client.get("$baseUrl/specialites/$cis").body()
    }

    suspend fun searchSpecialites(libelle: String, limit: Int = 10): List<MedicamentSpeResume> {
        return client.get("$baseUrl/specialites") {
            parameter("libelle", libelle)
            parameter("limit", limit)
        }.body()
    }

    suspend fun getPresentationByCip(cip13: String): MedicamentPresDetail {
        return client.get("$baseUrl/presentations/cip/$cip13").body()
    }

    suspend fun getPresentationsByCis(cis: Int): List<MedicamentPresDetail> {
        return client.get("$baseUrl/presentations/cis/$cis").body()
    }

    suspend fun searchPresentations(libelle: String, limit: Int = 10): List<MedicamentPresDetail> {
        val response = client.get("$baseUrl/presentations") {
            parameter("libelle", libelle)
            parameter("limit", limit)
        }
        return if (response.status == HttpStatusCode.NoContent) {
            emptyList()
        } else {
            response.body()
        }
    }

    suspend fun getSubstancesResumeByCis(cis: Int): List<MedicamentSubstanceResume> {
        return client.get("$baseUrl/substances/resume/cis/$cis").body()
    }

    suspend fun getSubstancesDetailByCis(cis: Int): List<MedicamentSubstanceDetail> {
        return client.get("$baseUrl/substances/detail/cis/$cis").body()
    }

    companion object {
        /**
         * Un seul client HTTP (pool de connexions OkHttp) pour toute l'application : il était auparavant recréé,
         * et jamais fermé, à chaque ViewModel.
         */
        private val sharedClient: HttpClient by lazy {
            HttpClient(OkHttp) {
                install(ContentNegotiation) {
                    gson()
                }
                install(Logging) {
                    // Jamais de corps de requête/réponse dans logcat en version publiée.
                    level = if (BuildConfig.DEBUG) LogLevel.INFO else LogLevel.NONE
                }
                install(HttpTimeout) {
                    requestTimeoutMillis = 15_000
                    connectTimeoutMillis = 15_000
                    socketTimeoutMillis = 15_000
                }
            }
        }

        /** Préfixe sous lequel le serveur expose les routes médicaments. */
        const val API_PREFIX = "/api/v1/medicament"

        /**
         * Construit l'URL de base de l'API à partir de la racine du serveur.
         * Tolère les espaces, un "/" final, et un préfixe déjà présent (ancienne configuration).
         */
        fun buildApiBaseUrl(serverRoot: String): String {
            val root = serverRoot.trim().trimEnd('/')
            return if (root.endsWith(API_PREFIX)) root else root + API_PREFIX
        }
    }
}
