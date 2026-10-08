package medicapp.server.interfaces

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.routing.*
import io.ktor.server.response.*
import medicapp.server.domain.service.MedicamentService

private const val PATH_CIS = "/cis/{cis}"
private const val ERR_CIS_INVALID = "CIS invalide"
private const val ERR_LIBELLE_MISSING = "L'argument libelle n'est pas renseigné"
private const val ERR_NOT_FOUND_CIS = "Aucun médicament trouvé"

/** Nombre de résultats par défaut / maximal d'une recherche (évite les requêtes volontairement massives). */
const val DEFAULT_LIMIT = 10
const val MAX_LIMIT = 50

/** Lit le paramètre `limit` : valeur par défaut si absent ou invalide, borné entre 1 et [MAX_LIMIT]. */
internal fun ApplicationCall.limitParam(): Int =
    (parameters["limit"]?.toIntOrNull() ?: DEFAULT_LIMIT).coerceIn(1, MAX_LIMIT)

private fun errNotFoundLibelle(libelle: String) =
    "Aucun médicament trouvé pour le libelle : $libelle"

fun Route.medicamentRoutes(service: MedicamentService) {
    route("/api/v1/medicament") {
        specialitesRoutes(service)
        presentationsRoutes(service)
        substancesRoutes(service)
    }
}

// ============== SPÉCIALITÉS ==============

private fun Route.specialitesRoutes(service: MedicamentService) {
    route("/specialites") {
        get("/{cis}") {
            val cis = call.parameters["cis"]?.toIntOrNull()
                ?: return@get call.respond(HttpStatusCode.BadRequest, ERR_CIS_INVALID)

            val result = service.getSpeResumeByCis(cis)
                ?: return@get call.respond(HttpStatusCode.NotFound, ERR_NOT_FOUND_CIS)

            call.respond(result)
        }

        get {
            val libelle = call.parameters["libelle"]
                ?: return@get call.respond(HttpStatusCode.BadRequest, "Paramètre 'libelle' manquant")

            val limit = call.limitParam()

            val result = service.searchSpeResumeByLibelle(libelle, limit)
            call.respond(result)
        }

        get("/completion") {
            val libelle = call.parameters["libelle"]
                ?: return@get call.respond(HttpStatusCode.BadRequest, "Paramètre 'libelle' manquant")

            val limit = call.limitParam()

            val result = service.getLibelleCompletion(libelle, limit)

            if (result.isEmpty()) {
                return@get call.respond(HttpStatusCode.NoContent)
            }
            call.respond(result)
        }

        specialitesStatusRoutes(service)
    }
}

private fun Route.specialitesStatusRoutes(service: MedicamentService) {
    route("/status") {
        get("/latest/{cis}") {
            val cis = call.parameters["cis"]?.toIntOrNull()
                ?: return@get call.respond(HttpStatusCode.BadRequest)

            val result = service.getLatestEventByCis(cis)
                ?: return@get call.respond(HttpStatusCode.NoContent)

            call.respond(result)
        }

        get("/all/{cis}") {
            val cis = call.parameters["cis"]?.toIntOrNull()
                ?: return@get call.respond(HttpStatusCode.BadRequest)

            val result = service.getAllEventsByCis(cis)

            if (result.isEmpty()) {
                return@get call.respond(HttpStatusCode.NoContent)
            }
            call.respond(result)
        }

        get("/marketed/{cis}") {
            val cis = call.parameters["cis"]?.toIntOrNull()
                ?: return@get call.respond(HttpStatusCode.BadRequest)

            val result = service.isSpeMarketed(cis)
            call.respond(result)
        }
    }
}

// ============== PRÉSENTATIONS ==============

private fun Route.presentationsRoutes(service: MedicamentService) {
    route("/presentations") {
        get("/cip/{cip13}") {
            val cip13 = call.parameters["cip13"].toString()

            val result = service.getPresDetailByCip(cip13)
                ?: return@get call.respond(HttpStatusCode.NoContent)

            call.respond(result)
        }

        get(PATH_CIS) {
            val cis = call.parameters["cis"]?.toIntOrNull()
                ?: return@get call.respond(HttpStatusCode.BadRequest)

            val result = service.getAllPresDetailByCis(cis)

            if (result.isEmpty()) {
                return@get call.respond(HttpStatusCode.NoContent)
            }
            call.respond(result)
        }

        get {
            val libelle = call.parameters["libelle"]
                ?: return@get call.respond(HttpStatusCode.BadRequest, ERR_LIBELLE_MISSING)

            val limit = call.limitParam()

            val detail = service.searchPresDetailByLibelle(libelle, limit)

            if (detail.isEmpty()) {
                return@get call.respond(HttpStatusCode.NoContent, errNotFoundLibelle(libelle))
            }
            call.respond(detail)
        }

        presentationsStatusRoutes(service)
    }
}

private fun Route.presentationsStatusRoutes(service: MedicamentService) {
    route("/status") {
        get("/latest/{cip13}") {
            val cip13 = call.parameters["cip13"]
                ?: return@get call.respond(HttpStatusCode.BadRequest)

            val result = service.getLatestEventByCip(cip13)
                ?: return@get call.respond(HttpStatusCode.NoContent)

            call.respond(result)
        }

        get("/all/{cip13}") {
            val cip13 = call.parameters["cip13"]
                ?: return@get call.respond(HttpStatusCode.BadRequest)

            val result = service.getAllEventsByCip(cip13)

            if (result.isEmpty()) {
                return@get call.respond(HttpStatusCode.NoContent)
            }
            call.respond(result)
        }

        get("/marketed/{cip13}") {
            val cip13 = call.parameters["cip13"]
                ?: return@get call.respond(HttpStatusCode.BadRequest)

            val result = service.isPresMarketed(cip13)
            call.respond(result)
        }
    }
}

// ============== SUBSTANCES ==============

private fun Route.substancesRoutes(service: MedicamentService) {
    route("/substances") {
        substancesResumeRoutes(service)
        substancesDetailRoutes(service)
    }
}

private fun Route.substancesResumeRoutes(service: MedicamentService) {
    route("/resume") {
        get(PATH_CIS) {
            val cis = call.parameters["cis"]?.toIntOrNull()
                ?: return@get call.respond(HttpStatusCode.BadRequest)

            val result = service.getSubstancesResumeByCis(cis)

            if (result.isEmpty()) {
                return@get call.respond(HttpStatusCode.NoContent)
            }
            call.respond(result)
        }

        get {
            val codeSubstance = call.parameters["codeSubstance"]
                ?: return@get call.respond(HttpStatusCode.BadRequest)

            val cis = call.parameters["cis"]?.toIntOrNull()
                ?: return@get call.respond(HttpStatusCode.BadRequest)

            val result = service.getSubstanceResumeByCisAndCode(cis, codeSubstance)
                ?: return@get call.respond(HttpStatusCode.NoContent)

            call.respond(result)
        }
    }
}

private fun Route.substancesDetailRoutes(service: MedicamentService) {
    route("/detail") {
        get(PATH_CIS) {
            val cis = call.parameters["cis"]?.toIntOrNull()
                ?: return@get call.respond(HttpStatusCode.BadRequest)

            val result = service.getSubstancesDetailByCis(cis)

            if (result.isEmpty()) {
                return@get call.respond(HttpStatusCode.NoContent)
            }
            call.respond(result)
        }

        get {
            val codeSubstance = call.parameters["codeSubstance"]
                ?: return@get call.respond(HttpStatusCode.BadRequest)

            val cis = call.parameters["cis"]?.toIntOrNull()
                ?: return@get call.respond(HttpStatusCode.BadRequest)

            val result = service.getSubstanceDetailByCisAndCode(cis, codeSubstance)
                ?: return@get call.respond(HttpStatusCode.NoContent)

            call.respond(result)
        }
    }
}