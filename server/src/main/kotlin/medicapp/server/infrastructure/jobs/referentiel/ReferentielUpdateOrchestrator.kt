package medicapp.server.infrastructure.jobs.referentiel

import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import medicapp.server.config.MedDatabase
import medicapp.server.config.StorageConfig
import medicapp.server.infrastructure.db.enums.ImportStatusEnum
import medicapp.server.infrastructure.db.requests.ReferentielTableManager
import medicapp.server.infrastructure.db.requests.dao.ReferentielImportStateDAO
import medicapp.server.infrastructure.db.tables.common.ReferentielImportStateTable
import medicapp.server.infrastructure.jobs.util.DownloadService
import medicapp.server.infrastructure.jobs.util.HashService
import medicapp.server.infrastructure.jobs.util.HtmlVersionChecker
import medicapp.server.config.logger
import medicapp.server.infrastructure.jobs.util.FileManager

class ReferentielUpdateOrchestrator(
    private val storage: StorageConfig,
    private val fileManager: FileManager = FileManager(),
    private val htmlVersionChecker: HtmlVersionChecker = HtmlVersionChecker()

) {

    // Initialisation du logger
    private val log = logger()


    suspend fun run(context: ReferentielContext) {

        log.info("Starting new jobs update of referentiel for datasource ${context.source}")

        // Chargement de la version des données actuelles contenu dans la base de donnée active
        val currentState = ReferentielImportStateDAO.getState(
            context.source,
            MedDatabase.ACTIVE
        )

        // On récupère la version et le code hash stocké
        val currentVersion = currentState[ReferentielImportStateTable.version]
        val currentHash = currentState[ReferentielImportStateTable.fileHash]

        log.info("[${context.source}] Checking if a new version is available for datasource")
        // On vérifie si on doit mettre à jour ou non
        val (shouldUpdate, newVersion) =
            htmlVersionChecker.checkForUpdate(context.checkUrl, currentVersion)

        if (!shouldUpdate) {
            log.info("[${context.source}] No new version available for datasource stoping job")
            return
        }


        val zipFile = storage.tempDir
            .resolve("${context.source}_$newVersion.zip")
            .toFile()

        log.info("[${context.source}] New version available, downloading ZIP file for datasource")
        // Si on doit mettre à jour, on télécharge la dernière version du fichier ZIP contenant les donénes source
        DownloadService.download(context.downloadUrl(newVersion), zipFile)
        // On vérifie que le hash du zip téléchargé soit différent de celui stocké en base
        if (HashService.isSame(zipFile, currentHash)) {
            log.info("[${context.source}] ZIP file downloaded and hash code stored in DB are the same, stoping job")
            log.info("[${context.source}] Cleaning up classpath data/ directory and temp/ directory")
            fileManager.cleanup(storage.tempDir)
            fileManager.cleanup(storage.dataDir)
            return
        }
        // On dézip le fichier téléchargé et on fait en sorte d'avoir les fichier CSV dans le dossier DATA
        log.info("[${context.source}] Preparing CSV source files")
        fileManager.prepareFiles(zipFile, context.source, storage)

        // Importation des données dans les tables temporaires situées dans la base de donnée med_db_temp
        log.info("[${context.source}] Importing CSV files data into database")
        context.importAction(storage, MedDatabase.TEMP)

        // On vérifie que les données importées sont plausibles avant de remplacer la production
        ReferentielTableManager.assertImportIsSane(context.source)

        // On applique notre logique de roulement des tables
        log.info("[${context.source}] Switching tables")
        ReferentielTableManager.switchTables(context.source)

        // On met à jour la dernière version des données importées dans la base de donéne med_db_active
        log.info("[${context.source}] Updating referentiel_import_state table for database med_db_active, version : $newVersion")
        ReferentielImportStateDAO.updateState(
            source = context.source,
            newVersion = newVersion,
            newFileHash = HashService.computeHash(zipFile),
            importedAt = Clock.System.now().toLocalDateTime(TimeZone.UTC),
            newStatus = ImportStatusEnum.SUCCESS,
            database = MedDatabase.ACTIVE
        )

        // On met à jour l'avant dernière version des données importées dans la base de donéne med_db_archive
        log.info("[${context.source}] Updating referentiel_import_state table for database med_db_archive, version ${currentState[ReferentielImportStateTable.version]}")
        ReferentielImportStateDAO.updateState(
            source = currentState[ReferentielImportStateTable.data_source],
            newVersion = currentState[ReferentielImportStateTable.version],
            newFileHash = currentState[ReferentielImportStateTable.fileHash],
            importedAt = currentState[ReferentielImportStateTable.importedAt],
            newStatus = currentState[ReferentielImportStateTable.status],
            database = MedDatabase.ARCHIVE
        )

        // On nettoie les fichiers installés/déplacés dans notre dossier temporaire temp/ et notre dossier contenant
        // les fichiers CSV sources data/
        log.info("[${context.source}] Cleaning up classpath data/ directory and temp/ directory")
        fileManager.cleanup(storage.tempDir)
        fileManager.cleanup(storage.dataDir)
    }

    suspend fun init() {
        val contextBuilder = ReferentielContextBuilder()

        val contexts = listOf(
            contextBuilder.buildRuimContext(),
            contextBuilder.buildSmsContext()
        )

        contexts.forEach { context ->

            val version = htmlVersionChecker.scrapeWebsiteVersion(context.checkUrl)!!

            val zipFile = storage.tempDir
                .resolve("${context.source}_$version.zip")
                .toFile()

            DownloadService.download(context.downloadUrl(version), zipFile)

            // Hash SHA-256 du contenu : doit être calculé de la même façon que dans run() pour que la
            // comparaison avec le ZIP téléchargé plus tard soit valide.
            val fileHash = HashService.computeHash(zipFile)

            fileManager.prepareFiles(zipFile, context.source, storage)

            context.importAction(storage, MedDatabase.ACTIVE)

            ReferentielImportStateDAO.insertState(
                database = MedDatabase.ACTIVE,
                source = context.source,
                version = version,
                fileHash = fileHash,
                importedAt = Clock.System.now().toLocalDateTime(TimeZone.UTC),
                status = ImportStatusEnum.SUCCESS
            )
        }
        fileManager.cleanup(storage.tempDir)
        fileManager.cleanup(storage.dataDir)
    }
}
