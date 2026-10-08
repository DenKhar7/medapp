package medicapp.server.infrastructure.jobs.referentiel

import medicapp.server.infrastructure.db.enums.DataSourceEnum
import medicapp.server.infrastructure.db.requests.ImportExecutionService
import medicapp.server.infrastructure.jobs.JobsConstants
import medicapp.server.infrastructure.jobs.JobsConstants.RUIM_URL
import medicapp.server.infrastructure.jobs.JobsConstants.SMS_URL

class ReferentielContextBuilder {
    fun buildRuimContext() : ReferentielContext {
        return ReferentielContext(
            source = DataSourceEnum.RUIM,
            checkUrl = RUIM_URL,
            downloadUrl = JobsConstants::ruimDownloadUrl,
            importAction = { storageConfig ,database -> ImportExecutionService.importRuimData(database, storageConfig) }
        )
    }

    fun buildSmsContext() : ReferentielContext {
        return ReferentielContext(
            source = DataSourceEnum.SMS,
            checkUrl = SMS_URL,
            downloadUrl = JobsConstants::smsDownloadUrl,
            importAction = { storageConfig ,database -> ImportExecutionService.importSmsData(database, storageConfig) }
        )
    }
}
