package medicapp.server.infrastructure.jobs.referentiel

import io.ktor.server.config.ApplicationConfig
import medicapp.server.config.loadStorageConfig

// Job pour le référentiel SMS
suspend fun updateSmsJob(appConfig: ApplicationConfig) {
    val storageConfig = loadStorageConfig(appConfig)

    val context = ReferentielContextBuilder()
        .buildSmsContext()

    val orchestrator = ReferentielUpdateOrchestrator(storageConfig)

    orchestrator.run(context)
}
