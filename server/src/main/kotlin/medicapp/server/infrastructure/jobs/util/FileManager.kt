package medicapp.server.infrastructure.jobs.util

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import medicapp.server.config.StorageConfig
import medicapp.server.infrastructure.db.enums.DataSourceEnum
import java.io.BufferedInputStream
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import kotlin.io.path.listDirectoryEntries
import java.io.FileOutputStream

class FileManager(
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    /** Taille décompressée maximale acceptée pour un ZIP (protection contre les "zip bombs"). */
    private val maxUnzippedBytes: Long = DEFAULT_MAX_UNZIPPED_BYTES,
    /** Nombre maximal d'entrées acceptées dans un ZIP. */
    private val maxEntries: Int = DEFAULT_MAX_ENTRIES
) {

    companion object {
        // Les référentiels RUIM/SMS font quelques dizaines de Mo décompressés : 1 Go laisse une large marge.
        const val DEFAULT_MAX_UNZIPPED_BYTES: Long = 1L shl 30
        const val DEFAULT_MAX_ENTRIES: Int = 1_000
    }

    /**
     * Méthode permettant de nettoyer les dossiers utilisés pour mettre à jour les données de la
     * base de donnée (dossier data/ et dossier temp/).
     * Cette fonction supprime tout le contenu d'un dossier.
     * @param dir : chemin du dossier à nettoyer
     */
    fun cleanup(dir : Path) {
        Files.walk(dir)
            .sorted(Comparator.reverseOrder()) // fichiers d'abord
            .filter { it != dir }
            .forEach { Files.delete(it) }
    }

    /**
     * Méthode permettant de préparer les fichiers CSV contenant les données source en les
     * placant dans le dossier data/. Plus particulièrement, cette fonction dézippe le dossier
     * téléchargé dans temp/.
     * Si c'est le référentiel RUIM qui est mis à jour, la fonction dézippe un second fichier ZIP
     * contenu dans le dossier dat/. Enfin, elle déplace tous les fichiers CSV contenus dans le dossier
     * obtenu dans le dossier data/.
     * Si c'est le référentiel SMS qui est mis à jour, la fonction déplace le fichier CSV contenu dans
     * le dossier dat/ vers le dossier data/.
     *
     * @param zipFile le fichier ZIP téléchargé au préalable
     * @param dataSource Le référentiel qui est mis à jour (RUIM ou SMS)
     */
    suspend fun prepareFiles(
        zipFile: File,
        dataSource: DataSourceEnum,
        storage: StorageConfig
    ) = withContext(ioDispatcher) {

        Files.createDirectories(storage.dataDir)
        Files.createDirectories(storage.tempDir)

        val extractDir = storage.tempDir.resolve(zipFile.nameWithoutExtension).toFile()
        unzip(Path.of(zipFile.path), Path.of(extractDir.path))

        val datDir = File(extractDir, "dat")
        require(datDir.exists()) { "Dossier dat introuvable : ${datDir.path}" }

        val csvSourceDir =
            if (dataSource == DataSourceEnum.RUIM) {
                val innerZip = datDir.listFiles { _, n -> n.endsWith(".zip") }
                    ?.firstOrNull() ?: error("Aucun zip interne trouvé")

                val innerExtract = storage.tempDir.resolve(innerZip.nameWithoutExtension).toFile()
                unzip(Path.of(innerZip.path), Path.of(innerExtract.path))
                innerExtract
            } else {
                datDir
            }

        csvSourceDir.toPath()
            .listDirectoryEntries("*.csv")
            .forEach { csv ->
                Files.move(
                    csv,
                    storage.dataDir.resolve(csv.fileName),
                    StandardCopyOption.REPLACE_EXISTING
                )
            }
    }


    /**
     * Méthode permettant de dézipper un fichier ZIP.
     *
     * @param zipFile Le chemin du fichier à dézipper
     * @param destinationDir Le chemin du dossier dans lequel placer les fichiers contenus dans le ZIP
     */
    fun unzip(zipFile: Path, destinationDir: Path) {
        Files.createDirectories(destinationDir)

        var totalBytes = 0L
        var entryCount = 0

        ZipInputStream(BufferedInputStream(Files.newInputStream(zipFile))).use { zis ->
            var entry: ZipEntry?

            while (zis.nextEntry.also { entry = it } != null) {
                val zipEntry = entry!!

                entryCount++
                require(entryCount <= maxEntries) { "ZIP refusé : plus de $maxEntries entrées" }

                // Protection Zip Slip (sécurité prod)
                val resolvedPath = destinationDir.resolve(zipEntry.name).normalize()
                require(resolvedPath.startsWith(destinationDir)) {
                    "Zip entry outside target dir: ${zipEntry.name}"
                }

                if (zipEntry.isDirectory) {
                    Files.createDirectories(resolvedPath)
                } else {
                    Files.createDirectories(resolvedPath.parent)

                    FileOutputStream(resolvedPath.toFile()).use { fos ->
                        val buffer = ByteArray(8192)
                        var read: Int
                        while (zis.read(buffer).also { read = it } != -1) {
                            totalBytes += read
                            require(totalBytes <= maxUnzippedBytes) {
                                "ZIP refusé : contenu décompressé supérieur à $maxUnzippedBytes octets"
                            }
                            fos.write(buffer, 0, read)
                        }
                    }
                }
                zis.closeEntry()
            }
        }
    }
}