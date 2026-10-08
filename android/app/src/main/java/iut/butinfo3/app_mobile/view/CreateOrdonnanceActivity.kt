package iut.butinfo3.app_mobile.view

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import iut.butinfo3.app_mobile.R
import iut.butinfo3.app_mobile.databinding.CreateOrdonnanceActivityBinding
import iut.butinfo3.app_mobile.model.entity.Ordonnance
import iut.butinfo3.app_mobile.utils.ViewModelFactory
import iut.butinfo3.app_mobile.view_model.OrdonnanceViewModel
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class CreateOrdonnanceActivity : AppCompatActivity() {

    private lateinit var binding: CreateOrdonnanceActivityBinding
    private val viewModel: OrdonnanceViewModel by viewModels { ViewModelFactory(this) }
    
    private var prescriptionDate: Date? = null
    private val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.FRANCE)

    private var editingOrdonnance: Ordonnance? = null
    private var capturedImagePath: String? = null
    private var ocrFullText: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = CreateOrdonnanceActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val ordonnanceId = intent.getIntExtra(EXTRA_ORDONNANCE_ID, -1).takeIf { it > 0 }
        capturedImagePath = intent.getStringExtra(EXTRA_IMAGE_PATH)
        ocrFullText = intent.getStringExtra(EXTRA_FULL_TEXT)
        val ocrDoctor = intent.getStringExtra(EXTRA_DOCTOR)
        val ocrAddress = intent.getStringExtra(EXTRA_ADDRESS)

        setupObservers()

        if (ordonnanceId != null) {
            // Mode edition
            viewModel.loadOrdonnance(ordonnanceId)
        } else {
            // Mode creation depuis scan ou saisie manuelle
            if (capturedImagePath != null) {
                // Image scannee disponible
                displayScannedImage(capturedImagePath!!)

                // Pre remplir depuis OCR si disponible
                ocrDoctor?.let { binding.etDoctorName.setText(it) }
                ocrAddress?.let { binding.etDoctorAddress.setText(it) }
            }
        }

        // Selecteur de date
        binding.etPrescriptionDate.setOnClickListener {
            showDatePicker()
        }

        binding.btnBack.setOnClickListener {
            finish()
        }

        binding.btnSave.setOnClickListener {
            saveOrdonnance()
        }
    }

    private fun setupObservers() {
        lifecycleScope.launch {
            viewModel.currentOrdonnance.collect { ordonnance ->
                ordonnance?.let { populateOrdonnanceFields(it) }
            }
        }

        lifecycleScope.launch {
            viewModel.operationSuccess.collect { operation ->
                operation?.let { handleOperationSuccess() }
            }
        }

        lifecycleScope.launch {
            viewModel.error.collect { errorMsg ->
                errorMsg?.let { handleError(it) }
            }
        }
    }

    private fun populateOrdonnanceFields(ordonnance: Ordonnance) {
        editingOrdonnance = ordonnance
        binding.etDoctorName.setText(ordonnance.doctorName)
        binding.etDoctorAddress.setText(ordonnance.doctorAddress)
        binding.etNotes.setText(ordonnance.notes)
        prescriptionDate = ordonnance.prescriptionDate
        prescriptionDate?.let { binding.etPrescriptionDate.setText(dateFormat.format(it)) }

        // Stocker chemin image depuis ordonnance existante
        if (capturedImagePath == null) {
            capturedImagePath = ordonnance.imagePath
        }

        // Afficher image si disponible
        capturedImagePath?.let { displayScannedImage(it) }
    }

    private fun handleOperationSuccess() {
        Toast.makeText(this, R.string.prescription_saved, Toast.LENGTH_SHORT).show()

        if (binding.cbCreateTreatmentAfter.isChecked) {
            editingOrdonnance?.let { navigateToTreatmentCreationFromPrescription(it) }
        } else {
            finish()
        }

        viewModel.clearOperationSuccess()
    }

    private fun handleError(errorMsg: String) {
        Toast.makeText(this, errorMsg, Toast.LENGTH_SHORT).show()
        if (errorMsg.contains("introuvable")) {
            finish()
        }
        viewModel.clearError()
    }

    // Afficher image scannee
    private fun displayScannedImage(imagePath: String) {
        val file = File(imagePath)
        if (file.exists() && file.canRead()) {
            try {
                // Charger image avec redimensionnement pour eviter OutOfMemoryError
                val bitmap = decodeSampledBitmapFromFile(file.absolutePath, 800, 1200)
                if (bitmap != null) {
                    binding.ivScannedDocument.visibility = View.VISIBLE
                    binding.tvNoDocument.visibility = View.GONE
                    binding.ivScannedDocument.setImageBitmap(bitmap)
                } else {
                    // Bitmap null fichier corrompu ou illisible
                    showImageError("Image corrompue ou illisible")
                }
            } catch (e: Exception) {
                // Gerer erreurs decodage image
                android.util.Log.e("CreateOrdonnance", "Error loading image: ${e.message}")
                showImageError("Erreur lors du chargement de l'image: ${e.message}")
            }
        } else {
            showImageError(if (!file.exists()) "Fichier introuvable" else "Fichier non accessible")
        }
    }

    // Fonction utilitaire pour decoder bitmap redimensionne et eviter OutOfMemoryError
    private fun decodeSampledBitmapFromFile(path: String, reqWidth: Int, reqHeight: Int): android.graphics.Bitmap? {
        // Decoder initialement avec inJustDecodeBounds true pour verifier dimensions
        val options = android.graphics.BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        android.graphics.BitmapFactory.decodeFile(path, options)

        // Calculer inSampleSize pour redimensionnement
        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight)

        // Decoder bitmap avec inSampleSize defini
        options.inJustDecodeBounds = false
        return android.graphics.BitmapFactory.decodeFile(path, options)
    }

    private fun calculateInSampleSize(
        options: android.graphics.BitmapFactory.Options,
        reqWidth: Int,
        reqHeight: Int
    ): Int {
        val (height: Int, width: Int) = options.run { outHeight to outWidth }
        var inSampleSize = 1

        if (height > reqHeight || width > reqWidth) {
            val halfHeight: Int = height / 2
            val halfWidth: Int = width / 2

            while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        }

        return inSampleSize
    }

    private fun showImageError(message: String) {
        binding.ivScannedDocument.visibility = View.GONE
        binding.tvNoDocument.visibility = View.VISIBLE
        binding.tvNoDocument.text = message
    }

    // Afficher selecteur de date
    private fun showDatePicker() {
        val calendar = Calendar.getInstance()
        if (prescriptionDate != null) {
            calendar.time = prescriptionDate!!
        }

        DatePickerDialog(
            this,
            { _, year, month, day ->
                calendar.set(year, month, day)
                prescriptionDate = calendar.time
                binding.etPrescriptionDate.setText(dateFormat.format(prescriptionDate!!))
            },
            calendar[Calendar.YEAR],
            calendar[Calendar.MONTH],
            calendar[Calendar.DAY_OF_MONTH]
        ).show()
    }

    // Sauvegarde l ordonnance en base de donnees
    private fun saveOrdonnance() {
        val doctorName = binding.etDoctorName.text.toString().trim()
        val doctorAddress = binding.etDoctorAddress.text.toString().trim()
        val notes = binding.etNotes.text.toString().trim()

        // Recupere l utilisateur actif via ViewModel
        val userId = viewModel.getActiveUserId() ?: run {
            Toast.makeText(this, R.string.prescription_error, Toast.LENGTH_SHORT).show()
            return
        }

        val existing = editingOrdonnance
        if (existing != null) {
            // Preserver chemin image existant si aucune nouvelle image capturee
            val finalImagePath = capturedImagePath ?: existing.imagePath
            val updated = existing.copy(
                doctorName = doctorName.ifBlank { null },
                doctorAddress = doctorAddress.ifBlank { null },
                prescriptionDate = prescriptionDate,
                imagePath = finalImagePath,
                notes = notes.ifBlank { null }
            )
            editingOrdonnance = updated
            viewModel.updateOrdonnance(updated)
        } else {
            // Creation
            val ordonnance = Ordonnance(
                userId = userId,
                doctorName = doctorName.ifBlank { null },
                doctorAddress = doctorAddress.ifBlank { null },
                prescriptionDate = prescriptionDate,
                scanDate = Date(),
                fullText = ocrFullText ?: "",
                imagePath = capturedImagePath,
                notes = notes.ifBlank { null }
            )
            editingOrdonnance = ordonnance
            viewModel.addOrdonnance(ordonnance)
        }
    }

    private fun navigateToTreatmentCreationFromPrescription(ordonnance: Ordonnance) {
        // Pre remplit donnees traitement avec infos ordonnance
        val startMillis = (ordonnance.prescriptionDate ?: Date()).time
        val instructions = buildString {
            ordonnance.doctorName?.let { append("Prescrit par: ").append(it).append("\n") }
            ordonnance.doctorAddress?.let { append("Adresse: ").append(it).append("\n") }
            ordonnance.notes?.let { append("\nNotes: ").append(it) }
        }

        Toast.makeText(this, "Veuillez sélectionner le médicament", Toast.LENGTH_SHORT).show()
        val intent = Intent(this, SelectMedicamentActivity::class.java).apply {
            putExtra(SelectMedicamentActivity.EXTRA_TARGET_USER_ID, ordonnance.userId)
            putExtra(SelectMedicamentActivity.EXTRA_PREFILL_START_DATE_MILLIS, startMillis)
            putExtra(SelectMedicamentActivity.EXTRA_PREFILL_INSTRUCTIONS, instructions)
            putExtra(SelectMedicamentActivity.EXTRA_NAVIGATE_TO_HOME_AFTER, true)
        }
        startActivity(intent)
        finish()
    }

    companion object {
        const val EXTRA_FULL_TEXT = "extra_full_text"
        const val EXTRA_DOCTOR = "extra_doctor"
        const val EXTRA_ADDRESS = "extra_address"
        const val EXTRA_ORDONNANCE_ID = "extra_ordonnance_id"
        const val EXTRA_IMAGE_PATH = "extra_image_path"
    }
}
