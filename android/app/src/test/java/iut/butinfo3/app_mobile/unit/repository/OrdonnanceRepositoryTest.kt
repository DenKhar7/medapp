package iut.butinfo3.app_mobile.unit.repository

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import iut.butinfo3.app_mobile.model.dao.OrdonnanceDao
import iut.butinfo3.app_mobile.model.entity.Ordonnance
import iut.butinfo3.app_mobile.model.repository.OrdonnanceRepository
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.util.Date

/** La photo d'une ordonnance (donnée de santé) disparaît avec l'ordonnance. */
class OrdonnanceRepositoryTest {

    @TempDir
    lateinit var tempDir: File

    private val dao = mockk<OrdonnanceDao>(relaxed = true)
    private val repository = OrdonnanceRepository(dao)

    private fun ordonnance(imagePath: String?) = Ordonnance(
        id = 1, userId = 1, scanDate = Date(), fullText = "texte", imagePath = imagePath
    )

    @Test
    fun deleteOrdonnance_removesRowAndItsScannedImage() = runTest {
        val image = File(tempDir, "ordonnance_scan_123.jpg").apply { writeText("jpeg") }
        coEvery { dao.deleteOrdonnance(any()) } returns Unit

        repository.deleteOrdonnance(ordonnance(image.absolutePath))

        coVerify(exactly = 1) { dao.deleteOrdonnance(any()) }
        assertFalse(image.exists(), "la photo doit être supprimée")
    }

    @Test
    fun deleteOrdonnance_neverDeletesFilesItDidNotCreate() = runTest {
        val foreign = File(tempDir, "photo_de_famille.jpg").apply { writeText("jpeg") }

        repository.deleteOrdonnance(ordonnance(foreign.absolutePath))

        assertTrue(foreign.exists(), "seuls les fichiers ordonnance_scan_* peuvent être supprimés")
    }

    @Test
    fun deleteOrdonnance_withoutImage_justDeletesTheRow() = runTest {
        repository.deleteOrdonnance(ordonnance(null))
        repository.deleteOrdonnance(ordonnance(""))

        coVerify(exactly = 2) { dao.deleteOrdonnance(any()) }
    }

    @Test
    fun deleteOrdonnance_toleratesAnImageAlreadyGone() = runTest {
        repository.deleteOrdonnance(ordonnance(File(tempDir, "ordonnance_scan_999.jpg").absolutePath))

        coVerify(exactly = 1) { dao.deleteOrdonnance(any()) }
    }
}
