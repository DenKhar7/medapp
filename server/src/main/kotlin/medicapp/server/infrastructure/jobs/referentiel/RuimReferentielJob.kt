package medicapp.server.infrastructure.jobs.referentiel

import io.ktor.server.config.ApplicationConfig
import medicapp.server.config.loadStorageConfig


// Job pour le référentiel RUIM
suspend fun updateRuimJob(appConfig: ApplicationConfig) {

    val storageConfig = loadStorageConfig(appConfig)

    val context = ReferentielContextBuilder()
        .buildRuimContext()

    val orchestrator = ReferentielUpdateOrchestrator(storageConfig)

    orchestrator.run(context)
}

