package medicapp.server.infrastructure.jobs.util

import org.junit.jupiter.api.io.TempDir
import java.io.FileOutputStream
import java.nio.file.Path
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/** Protection contre les "zip bombs" : taille décompressée et nombre d'entrées plafonnés. */
class FileManagerLimitsTest {

    @TempDir
    lateinit var tempDir: Path

    private fun zipOf(name: String, entries: Map<String, ByteArray>): Path {
        val zip = tempDir.resolve(name)
        ZipOutputStream(FileOutputStream(zip.toFile())).use { zos ->
            entries.forEach { (entryName, bytes) ->
                zos.putNextEntry(ZipEntry(entryName))
                zos.write(bytes)
                zos.closeEntry()
            }
        }
        return zip
    }

    @Test
    fun `un zip dont le contenu depasse la limite est refuse`() {
        // 1 Mo de zéros se compresse en quelques Ko : c'est la taille DÉCOMPRESSÉE qui est contrôlée
        val zip = zipOf("bomb.zip", mapOf("big.bin" to ByteArray(1_000_000)))
        val manager = FileManager(maxUnzippedBytes = 100_000)

        val error = assertFailsWith<IllegalArgumentException> { manager.unzip(zip, tempDir.resolve("out")) }

        assertTrue("décompressé" in error.message.orEmpty())
    }

    @Test
    fun `la limite s applique au total de toutes les entrees`() {
        val zip = zipOf("many.zip", (1..5).associate { "f$it.bin" to ByteArray(30_000) })
        val manager = FileManager(maxUnzippedBytes = 100_000)

        assertFailsWith<IllegalArgumentException> { manager.unzip(zip, tempDir.resolve("out2")) }
    }

    @Test
    fun `un zip avec trop d entrees est refuse`() {
        val zip = zipOf("flood.zip", (1..6).associate { "f$it.txt" to "x".toByteArray() })
        val manager = FileManager(maxEntries = 5)

        assertFailsWith<IllegalArgumentException> { manager.unzip(zip, tempDir.resolve("out3")) }
    }

    @Test
    fun `un zip normal reste accepte avec les limites par defaut`() {
        val zip = zipOf("ok.zip", mapOf("a.csv" to "col\n1\n".toByteArray()))
        val dest = tempDir.resolve("out4")

        FileManager().unzip(zip, dest)

        assertEquals("col\n1\n", dest.resolve("a.csv").toFile().readText())
    }
}
