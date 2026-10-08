package iut.butinfo3.app_mobile.view

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import iut.butinfo3.app_mobile.R
import iut.butinfo3.app_mobile.databinding.OrdonnanceActivityBinding
import iut.butinfo3.app_mobile.model.database.AppDatabase
import iut.butinfo3.app_mobile.model.repository.AuthRepository
import iut.butinfo3.app_mobile.model.repository.OrdonnanceRepository
import iut.butinfo3.app_mobile.model.repository.CameraRepository
import iut.butinfo3.app_mobile.view_model.OrdonnanceScreenViewModel
import iut.butinfo3.app_mobile.model.OcrResult
import iut.butinfo3.app_mobile.utils.NavigationHelper
import iut.butinfo3.app_mobile.view.adapter.OrdonnanceAdapter
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions
import com.google.mlkit.vision.documentscanner.GmsDocumentScanning
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult
import kotlinx.coroutines.launch

class OrdonnanceActivity : AppCompatActivity() {
    private lateinit var navigationHelper: NavigationHelper

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: OrdonnanceAdapter
    private lateinit var binding: OrdonnanceActivityBinding

    private val viewModel: OrdonnanceScreenViewModel by viewModels {
        object : androidx.lifecycle.ViewModelProvider.Factory {
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                val db = AppDatabase.getDatabase(this@OrdonnanceActivity)
                val authRepo = AuthRepository(this@OrdonnanceActivity, db.userDao())
                val ordoRepo = OrdonnanceRepository(db.ordonnanceDao())
                val cameraRepo = CameraRepository(this@OrdonnanceActivity)
                @Suppress("UNCHECKED_CAST")
                return OrdonnanceScreenViewModel(ordoRepo, cameraRepo, authRepo) as T
            }
        }
    }

    private val scanLauncher = registerForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode != RESULT_OK) {
            return@registerForActivityResult
        }
        val scanResult = GmsDocumentScanningResult.fromActivityResultIntent(result.data)
        val page = scanResult?.pages?.firstOrNull()
        val imageUri = page?.imageUri
        if (imageUri == null) {
            Toast.makeText(this, "Scan annulé ou vide", Toast.LENGTH_SHORT).show()
            return@registerForActivityResult
        }
        // Afficher progression si barre de progression presente
        viewModel.processImage(imageUri)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = OrdonnanceActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupRecyclerView()
        setupCardClicks()
        setupBottomNavigation()
        observeViewModel()
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.ordonnances.collect { list ->
                        adapter.submitList(list)
                    }
                }
                launch {
                    viewModel.ocrResult.collect { result ->
                        handleOcrResult(result)
                    }
                }
                launch {
                    viewModel.error.collect { errorMsg ->
                        Toast.makeText(this@OrdonnanceActivity, errorMsg, Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    private fun handleOcrResult(result: OcrResult) {
        val intent = Intent(this@OrdonnanceActivity, CreateOrdonnanceActivity::class.java).apply {
            putExtra(CreateOrdonnanceActivity.EXTRA_IMAGE_PATH, result.file.absolutePath)
            putExtra(CreateOrdonnanceActivity.EXTRA_FULL_TEXT, result.text)
            putExtra(CreateOrdonnanceActivity.EXTRA_DOCTOR, result.doctor)
            putExtra(CreateOrdonnanceActivity.EXTRA_ADDRESS, result.address)
        }
        startActivity(intent)
    }

    private fun setupRecyclerView() {
        recyclerView = findViewById(R.id.recyclerOrdonnances)
        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = OrdonnanceAdapter(
            ordonnances = emptyList(),
            onEditClick = { ordonnance ->
                startActivity(
                    Intent(this, CreateOrdonnanceActivity::class.java)
                        .putExtra(CreateOrdonnanceActivity.EXTRA_ORDONNANCE_ID, ordonnance.id)
                )
            },
            onDeleteClick = { ordonnance ->
                showDeleteConfirmation(ordonnance)
            }
        )
        recyclerView.adapter = adapter
        viewModel.loadOrdonnancesForActiveUser()
    }

    private fun showDeleteConfirmation(ordonnance: iut.butinfo3.app_mobile.model.entity.Ordonnance) {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Supprimer l'ordonnance")
            .setMessage("Voulez-vous vraiment supprimer cette ordonnance de ${ordonnance.doctorName ?: "Médecin non spécifié"} ?")
            .setPositiveButton("Supprimer") { _, _ ->
                viewModel.deleteOrdonnance(ordonnance)
                Toast.makeText(this, "Ordonnance supprimée", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Annuler", null)
            .show()
    }

    private fun setupCardClicks() {
        findViewById<CardView>(R.id.cvSaisie)?.setOnClickListener {
            startActivity(Intent(this, CreateOrdonnanceActivity::class.java))
        }

        findViewById<CardView>(R.id.cvScan)?.setOnClickListener {
            launchDocumentScanner()
        }
    }

    private fun launchDocumentScanner() {
        val options = GmsDocumentScannerOptions.Builder()
            .setScannerMode(GmsDocumentScannerOptions.SCANNER_MODE_FULL)
            .setGalleryImportAllowed(true)
            .setPageLimit(1)
            .setResultFormats(GmsDocumentScannerOptions.RESULT_FORMAT_JPEG)
            .build()
        val scanner = GmsDocumentScanning.getClient(options)
        scanner.getStartScanIntent(this)
            .addOnSuccessListener { intentSender ->
                scanLauncher.launch(IntentSenderRequest.Builder(intentSender).build())
            }
            .addOnFailureListener {
                Toast.makeText(this, "Impossible de lancer le scanner", Toast.LENGTH_SHORT).show()
            }
    }

    // Toute la logique fichier OCR et DB deplacee vers ViewModel et Repositories

    private fun setupBottomNavigation() {
        navigationHelper = NavigationHelper(
            binding.navHome,
            binding.navUserTreatments,
            binding.navMedicaments,
            binding.navOrdonnances,
            binding.navPathologie
        )
        navigationHelper.init(NavigationHelper.NAV_HOME)

        binding.navHome.setOnClickListener {
            startActivity(Intent(this, HomeActivity::class.java))
        }

        binding.navUserTreatments.setOnClickListener {
            startActivity(Intent(this, UserTreatmentsActivity::class.java))
        }

        binding.navMedicaments.setOnClickListener {
            startActivity(Intent(this, MedicamentActivity::class.java))
        }

        binding.navPathologie.setOnClickListener {
            startActivity(Intent(this, PathologieActivity::class.java))
        }
    }

    override fun onResume() {
        super.onResume()
        navigationHelper.setActiveTab(NavigationHelper.NAV_ORDONNANCES)
    }
}
