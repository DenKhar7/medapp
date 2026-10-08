package medicapp.server.infrastructure.jobs.util

import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import medicapp.server.config.StorageConfig
import medicapp.server.infrastructure.db.enums.DataSourceEnum
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.io.TempDir
import java.io.FileOutputStream
import java.nio.file.Files
import java.nio.file.Path
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.test.assertFailsWith

class FileManagerTest {

    @TempDir
    lateinit var tempDir: Path

    private lateinit var fileManager: FileManager
    private val testDispatcher = StandardTestDispatcher()

    @BeforeEach
    fun setup() {
        fileManager = FileManager(testDispatcher)
    }

    @AfterEach
    fun tearDown() {
        // Cleanup est gere par @TempDir
    }

    // ─────────────────────────────────────────────────────────────
    // Tests cleanup()
    // ─────────────────────────────────────────────────────────────

    @Test
    fun `cleanup supprime tous les fichiers du dossier`() {
        // Arrange
        val dir = tempDir.resolve("cleanupTest")
        Files.createDirectories(dir)
        Files.createFile(dir.resolve("file1.txt"))
        Files.createFile(dir.resolve("file2.txt"))

        // Act
        fileManager.cleanup(dir)

        // Assert
        assertTrue(Files.exists(dir))
        assertEquals(0, Files.list(dir).count())
    }

    @Test
    fun `cleanup supprime les sous-dossiers recursivement`() {
        // Arrange
        val dir = tempDir.resolve("cleanupRecursive")
        val subDir = dir.resolve("subdir")
        Files.createDirectories(subDir)
        Files.createFile(subDir.resolve("nested.txt"))
        Files.createFile(dir.resolve("root.txt"))

        // Act
        fileManager.cleanup(dir)

        // Assert
        assertTrue(Files.exists(dir))
        assertFalse(Files.exists(subDir))
        assertEquals(0, Files.list(dir).count())
    }

    @Test
    fun `cleanup sur dossier vide ne leve pas d exception`() {
        // Arrange
        val dir = tempDir.resolve("emptyDir")
        Files.createDirectories(dir)

        // Act & Assert - ne doit pas lever d'exception
        fileManager.cleanup(dir)

        assertTrue(Files.exists(dir))
    }

    @Test
    fun `cleanup supprime arborescence profonde`() {
        // Arrange
        val dir = tempDir.resolve("deepTree")
        val level1 = dir.resolve("level1")
        val level2 = level1.resolve("level2")
        val level3 = level2.resolve("level3")
        Files.createDirectories(level3)
        Files.createFile(level3.resolve("deep.txt"))
        Files.createFile(level1.resolve("mid.txt"))

        // Act
        fileManager.cleanup(dir)

        // Assert
        assertTrue(Files.exists(dir))
        assertFalse(Files.exists(level1))
    }

    // ─────────────────────────────────────────────────────────────
    // Tests unzip()
    // ─────────────────────────────────────────────────────────────

    @Test
    fun `unzip extrait les fichiers correctement`() {
        // Arrange
        val zipFile = createTestZip("basic.zip", mapOf(
            "file1.txt" to "content1",
            "file2.txt" to "content2"
        ))
        val destDir = tempDir.resolve("extracted")

        // Act
        fileManager.unzip(zipFile, destDir)

        // Assert
        assertTrue(Files.exists(destDir.resolve("file1.txt")))
        assertTrue(Files.exists(destDir.resolve("file2.txt")))
        assertEquals("content1", Files.readString(destDir.resolve("file1.txt")))
        assertEquals("content2", Files.readString(destDir.resolve("file2.txt")))
    }

    @Test
    fun `unzip cree les sous-dossiers necessaires`() {
        // Arrange
        val zipFile = createTestZip("nested.zip", mapOf(
            "folder/subfolder/file.txt" to "nested content"
        ))
        val destDir = tempDir.resolve("nestedExtract")

        // Act
        fileManager.unzip(zipFile, destDir)

        // Assert
        assertTrue(Files.exists(destDir.resolve("folder/subfolder/file.txt")))
        assertEquals("nested content", Files.readString(destDir.resolve("folder/subfolder/file.txt")))
    }

    @Test
    fun `unzip gere les dossiers dans le ZIP`() {
        // Arrange
        val zipFile = tempDir.resolve("withDirs.zip")
        ZipOutputStream(FileOutputStream(zipFile.toFile())).use { zos ->
            // Ajouter un dossier
            zos.putNextEntry(ZipEntry("mydir/"))
            zos.closeEntry()
            // Ajouter un fichier dans le dossier
            zos.putNextEntry(ZipEntry("mydir/file.txt"))
            zos.write("dir content".toByteArray())
            zos.closeEntry()
        }
        val destDir = tempDir.resolve("dirExtract")

        // Act
        fileManager.unzip(zipFile, destDir)

        // Assert
        assertTrue(Files.isDirectory(destDir.resolve("mydir")))
        assertTrue(Files.exists(destDir.resolve("mydir/file.txt")))
    }

    @Test
    fun `unzip protection Zip Slip bloque les chemins malveillants`() {
        // Arrange - creation manuelle d'un ZIP avec chemin malveillant
        val zipFile = tempDir.resolve("malicious.zip")
        ZipOutputStream(FileOutputStream(zipFile.toFile())).use { zos ->
            zos.putNextEntry(ZipEntry("../../../etc/passwd"))
            zos.write("malicious content".toByteArray())
            zos.closeEntry()
        }
        val destDir = tempDir.resolve("safe")

        // Act & Assert
        val exception = assertFailsWith<IllegalArgumentException> {
            fileManager.unzip(zipFile, destDir)
        }
        assertTrue(exception.message!!.contains("Zip entry outside target dir"))
    }

    @Test
    fun `unzip cree le dossier de destination si inexistant`() {
        // Arrange
        val zipFile = createTestZip("create.zip", mapOf("test.txt" to "test"))
        val destDir = tempDir.resolve("nonexistent/nested/dir")

        // Act
        fileManager.unzip(zipFile, destDir)

        // Assert
        assertTrue(Files.exists(destDir))
        assertTrue(Files.exists(destDir.resolve("test.txt")))
    }

    @Test
    fun `unzip gere les fichiers binaires`() {
        // Arrange
        val binaryContent = byteArrayOf(0x00, 0x01, 0xFF.toByte(), 0xFE.toByte())
        val zipFile = tempDir.resolve("binary.zip")
        ZipOutputStream(FileOutputStream(zipFile.toFile())).use { zos ->
            zos.putNextEntry(ZipEntry("binary.bin"))
            zos.write(binaryContent)
            zos.closeEntry()
        }
        val destDir = tempDir.resolve("binaryExtract")

        // Act
        fileManager.unzip(zipFile, destDir)

        // Assert
        val extracted = Files.readAllBytes(destDir.resolve("binary.bin"))
        assertTrue(binaryContent.contentEquals(extracted))
    }

    // ─────────────────────────────────────────────────────────────
    // Tests prepareFiles() - RUIM
    // ─────────────────────────────────────────────────────────────

    @Test
    fun `prepareFiles RUIM extrait et deplace les fichiers CSV`() = runTest(testDispatcher) {
        // Arrange
        val dataDir = tempDir.resolve("data")
        val tempStorageDir = tempDir.resolve("temp")
        Files.createDirectories(dataDir)
        Files.createDirectories(tempStorageDir)

        val storage = StorageConfig(tempDir = tempStorageDir, dataDir = dataDir)

        // Creer un ZIP interne avec des fichiers CSV
        val innerZipPath = tempDir.resolve("inner.zip")
        ZipOutputStream(FileOutputStream(innerZipPath.toFile())).use { zos ->
            zos.putNextEntry(ZipEntry("data1.csv"))
            zos.write("col1,col2\nval1,val2".toByteArray())
            zos.closeEntry()
            zos.putNextEntry(ZipEntry("data2.csv"))
            zos.write("col1,col2\nval3,val4".toByteArray())
            zos.closeEntry()
        }

        // Creer le ZIP principal avec structure dat/inner.zip
        val mainZip = tempStorageDir.resolve("RUIM_v1.0.zip").toFile()
        ZipOutputStream(FileOutputStream(mainZip)).use { zos ->
            zos.putNextEntry(ZipEntry("dat/"))
            zos.closeEntry()
            zos.putNextEntry(ZipEntry("dat/inner.zip"))
            zos.write(Files.readAllBytes(innerZipPath))
            zos.closeEntry()
        }

        // Act
        fileManager.prepareFiles(mainZip, DataSourceEnum.RUIM, storage)

        // Assert
        assertTrue(Files.exists(dataDir.resolve("data1.csv")))
        assertTrue(Files.exists(dataDir.resolve("data2.csv")))
    }

    // ─────────────────────────────────────────────────────────────
    // Tests prepareFiles() - SMS
    // ─────────────────────────────────────────────────────────────

    @Test
    fun `prepareFiles SMS deplace directement les fichiers CSV du dossier dat`() = runTest(testDispatcher) {
        // Arrange
        val dataDir = tempDir.resolve("data")
        val tempStorageDir = tempDir.resolve("temp")
        Files.createDirectories(dataDir)
        Files.createDirectories(tempStorageDir)

        val storage = StorageConfig(tempDir = tempStorageDir, dataDir = dataDir)

        // Creer le ZIP avec structure dat/fichier.csv (sans ZIP interne)
        val mainZip = tempStorageDir.resolve("SMS_v1.0.zip").toFile()
        ZipOutputStream(FileOutputStream(mainZip)).use { zos ->
            zos.putNextEntry(ZipEntry("dat/"))
            zos.closeEntry()
            zos.putNextEntry(ZipEntry("dat/substances.csv"))
            zos.write("sms_id,name\n1,Paracetamol".toByteArray())
            zos.closeEntry()
        }

        // Act
        fileManager.prepareFiles(mainZip, DataSourceEnum.SMS, storage)

        // Assert
        assertTrue(Files.exists(dataDir.resolve("substances.csv")))
    }

    @Test
    fun `prepareFiles echoue si dossier dat absent`() = runTest(testDispatcher) {
        // Arrange
        val dataDir = tempDir.resolve("data")
        val tempStorageDir = tempDir.resolve("temp")
        Files.createDirectories(dataDir)
        Files.createDirectories(tempStorageDir)

        val storage = StorageConfig(tempDir = tempStorageDir, dataDir = dataDir)

        // Creer un ZIP sans dossier dat
        val mainZip = tempStorageDir.resolve("invalid.zip").toFile()
        ZipOutputStream(FileOutputStream(mainZip)).use { zos ->
            zos.putNextEntry(ZipEntry("other/"))
            zos.closeEntry()
            zos.putNextEntry(ZipEntry("other/file.csv"))
            zos.write("content".toByteArray())
            zos.closeEntry()
        }

        // Act & Assert
        val exception = assertFailsWith<IllegalArgumentException> {
            fileManager.prepareFiles(mainZip, DataSourceEnum.SMS, storage)
        }
        assertTrue(exception.message!!.contains("Dossier dat introuvable"))
    }

    // ─────────────────────────────────────────────────────────────
    // Methode helper pour creer des ZIPs de test
    // ─────────────────────────────────────────────────────────────

    private fun createTestZip(name: String, files: Map<String, String>): Path {
        val zipPath = tempDir.resolve(name)
        ZipOutputStream(FileOutputStream(zipPath.toFile())).use { zos ->
            for ((fileName, content) in files) {
                // Creer les dossiers parents si necessaire
                val parts = fileName.split("/")
                if (parts.size > 1) {
                    var currentPath = ""
                    for (i in 0 until parts.size - 1) {
                        currentPath += parts[i] + "/"
                        try {
                            zos.putNextEntry(ZipEntry(currentPath))
                            zos.closeEntry()
                        } catch (_: Exception) {
                            // Dossier deja ajoute
                        }
                    }
                }
                zos.putNextEntry(ZipEntry(fileName))
                zos.write(content.toByteArray())
                zos.closeEntry()
            }
        }
        return zipPath
    }
}
