package medicapp.server.interfaces

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.ratelimit.RateLimitName
import io.ktor.server.plugins.ratelimit.rateLimit
import io.ktor.server.plugins.swagger.swaggerUI
import io.ktor.server.response.*
import io.ktor.server.routing.*
import medicapp.server.config.ApiConfig
import medicapp.server.domain.service.MedicamentService

/** Nom du limiteur de débit appliqué aux routes de l'API. */
val API_RATE_LIMIT = RateLimitName("api")

fun Application.configureRouting(
    medicamentService: MedicamentService,
    apiConfig: ApiConfig,
    /** Retourne true si les dépendances (base de données) répondent ; utilisé par /health. */
    healthCheck: suspend () -> Boolean = { true }
) {
    routing {

        get("/health") {
            if (healthCheck()) {
                call.respondText("OK", status = HttpStatusCode.OK)
            } else {
                call.respondText("DATABASE UNAVAILABLE", status = HttpStatusCode.ServiceUnavailable)
            }
        }

        swaggerUI(path = "swagger", swaggerFile = "openapi/documentation.yaml")

        rateLimit(API_RATE_LIMIT) {
            medicamentRoutes(medicamentService)
        }
    }
}
