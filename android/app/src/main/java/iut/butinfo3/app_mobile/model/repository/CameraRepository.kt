package iut.butinfo3.app_mobile.model.repository

import android.content.Context
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.File

class CameraRepository(
    private val context: Context,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    suspend fun saveImageFromUri(uri: Uri): File = withContext(ioDispatcher) {
        // Stockage interne privé : non accessible aux autres applications, contrairement au stockage externe.
        val dir = File(context.filesDir, PRESCRIPTIONS_DIR).apply { mkdirs() }
        val dest = File(dir, "${SCAN_PREFIX}${System.currentTimeMillis()}.jpg")
        context.contentResolver.openInputStream(uri).use { input ->
            requireNotNull(input) { "InputStream null" }
            dest.outputStream().use { output -> input.copyTo(output) }
        }
        dest
    }

    suspend fun performOCR(imageFile: File): String = withContext(ioDispatcher) {
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        val image = InputImage.fromFilePath(context, Uri.fromFile(imageFile))
        recognizer.process(image).await().text
    }

    fun extractDoctorInfo(text: String): Pair<String?, String?> {
        val lines = text.split("\n").map { it.trim() }.filter { it.isNotBlank() }
        if (lines.isEmpty()) return null to null

        var doctorName: String? = null
        var doctorAddress: String? = null

        for (i in lines.indices) {
            val line = lines[i]
            if (line.contains("Dr", ignoreCase = true) || 
                line.contains("Docteur", ignoreCase = true) ||
                line.contains("Médecin", ignoreCase = true)) {
                doctorName = line
                if (i + 1 < lines.size) {
                    doctorAddress = lines.subList(i + 1, minOf(i + 3, lines.size)).joinToString(", ")
                }
                break
            }
        }
        if (doctorName == null && lines.isNotEmpty()) {
            doctorName = lines[0]
            if (lines.size > 1) {
                doctorAddress = lines.subList(1, minOf(3, lines.size)).joinToString(", ")
            }
        }
        return doctorName to doctorAddress
    }

    companion object {
        const val PRESCRIPTIONS_DIR = "prescriptions"
        const val SCAN_PREFIX = "ordonnance_scan_"
    }
}