package medicapp.server.infrastructure.jobs.referentiel

import kotlinx.coroutines.CancellationException
import medicapp.server.config.logger
import medicapp.server.infrastructure.db.enums.DataSourceEnum
import medicapp.server.infrastructure.db.requests.dao.ReferentielImportStateDAO

class ReferentielJobs(
    private val orchestrator: ReferentielUpdateOrchestrator,
    /** Enregistre l'échec en base ; injectable pour les tests. */
    private val recordFailure: suspend (DataSourceEnum, String) -> Unit = { source, reason ->
        ReferentielImportStateDAO.markFailed(source, reason)
    }
) {

    private val log = logger()

    suspend fun updateRuimJob() {
        runSafely(ReferentielContextBuilder().buildRuimContext())
    }

    suspend fun updateSmsJob() {
        runSafely(ReferentielContextBuilder().buildSmsContext())
    }

    /**
     * Exécute une mise à jour sans jamais laisser remonter d'exception : un échec est journalisé et
     * enregistré (statut FAILED + message) dans referentiel_import_state, les données en production
     * restant celles de la dernière mise à jour réussie. Le prochain passage du job réessaiera.
     */
    private suspend fun runSafely(context: ReferentielContext) {
        try {
            orchestrator.run(context)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            log.error("[${context.source}] Mise à jour du référentiel en échec : ${e.message}", e)
            try {
                recordFailure(context.source, "${e::class.simpleName}: ${e.message}")
            } catch (inner: Exception) {
                log.error("[${context.source}] Impossible d'enregistrer l'échec en base : ${inner.message}")
            }
        }
    }
}
