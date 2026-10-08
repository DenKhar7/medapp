package iut.butinfo3.app_mobile.view

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import iut.butinfo3.app_mobile.databinding.SelectMedicamentActivityBinding
import iut.butinfo3.app_mobile.model.database.AppDatabase
import iut.butinfo3.app_mobile.model.repository.MedicamentRepository
import iut.butinfo3.app_mobile.view.adapter.SelectMedicamentAdapter
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.activity.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import iut.butinfo3.app_mobile.model.api.MedicamentApiClient
import iut.butinfo3.app_mobile.model.entity.MedicamentSpeResume
import iut.butinfo3.app_mobile.model.repository.AuthRepository
import iut.butinfo3.app_mobile.view_model.SelectMedicamentViewModel

class SelectMedicamentActivity : AppCompatActivity() {
    private lateinit var binding: SelectMedicamentActivityBinding
    private lateinit var adapter: SelectMedicamentAdapter
    private var prefillStartDateMillis: Long = -1L
    private var prefillInstructions: String? = null
    private var navigateToHomeAfter: Boolean = false
    private var searchJob: Job? = null
    private val viewModel: SelectMedicamentViewModel by viewModels {
        object : androidx.lifecycle.ViewModelProvider.Factory {
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                val dao = AppDatabase.getDatabase(this@SelectMedicamentActivity).medicamentDao()
                val apiClient = MedicamentApiClient()
                @Suppress("UNCHECKED_CAST")
                return SelectMedicamentViewModel(MedicamentRepository(dao, apiClient)) as T
            }
        }
    }
    private val authRepository: AuthRepository by lazy {
        val db = AppDatabase.getDatabase(this)
        AuthRepository(this, db.userDao())
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = SelectMedicamentActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val selectedUserId = authRepository.getActiveUserId()
        prefillStartDateMillis = intent.getLongExtra(EXTRA_PREFILL_START_DATE_MILLIS, -1L)
        prefillInstructions = intent.getStringExtra(EXTRA_PREFILL_INSTRUCTIONS)
        navigateToHomeAfter = intent.getBooleanExtra(EXTRA_NAVIGATE_TO_HOME_AFTER, false)

        if (selectedUserId == null || selectedUserId <= 0) {
            Toast.makeText(this, "Utilisateur introuvable.", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        binding.btnBack.setOnClickListener { finish() }

        adapter = SelectMedicamentAdapter(emptyList()) { cis, label ->
            lifecycleScope.launch {
                val ensured = viewModel.ensurePresentationInDb(cis)
                if (!ensured) {
                    Toast.makeText(this@SelectMedicamentActivity, "Erreur lors de la récupération de la présentation.", Toast.LENGTH_SHORT).show()
                    return@launch
                }

                // Get the presentation (CIP13) from the database
                val presentations = viewModel.getPresentationsByCis(cis)
                if (presentations.isEmpty()) {
                    Toast.makeText(this@SelectMedicamentActivity, "Aucune présentation trouvée pour ce médicament.", Toast.LENGTH_SHORT).show()
                    return@launch
                }

                val firstPresentation = presentations.first()

                val intent = Intent(this@SelectMedicamentActivity, AddTreatmentActivity::class.java).apply {
                    putExtra("EXTRA_CIP", firstPresentation.cip13)
                    putExtra("EXTRA_NAME", label)
                    putExtra("EXTRA_TARGET_USER_ID", selectedUserId)
                    if (prefillStartDateMillis > 0L) {
                        putExtra("EXTRA_PREFILL_START_DATE_MILLIS", prefillStartDateMillis)
                    }
                    putExtra("EXTRA_PREFILL_INSTRUCTIONS", prefillInstructions)
                    putExtra("EXTRA_NAVIGATE_TO_HOME_AFTER", navigateToHomeAfter)
                }
                startActivity(intent)
                finish()
            }
        }

        binding.recyclerMedicaments.layoutManager = LinearLayoutManager(this)
        binding.recyclerMedicaments.adapter = adapter

        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
            override fun afterTextChanged(s: Editable?) {
                scheduleSearch(s?.toString().orEmpty())
            }
        })

        observeViewModel()
        // Initial load
        scheduleSearch(binding.etSearch.text?.toString().orEmpty(), immediate = true)
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.medicaments.collect { list ->
                        handleMedicamentsList(list)
                    }
                }
                launch {
                    viewModel.error.collect { errorMsg ->
                        errorMsg?.let {
                            Toast.makeText(this@SelectMedicamentActivity, it, Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }
    }

    private fun handleMedicamentsList(list: List<MedicamentSpeResume>) {
        adapter.submitList(list)
        if (list.isEmpty()) {
            showEmptyState()
        } else {
            binding.tvEmpty.visibility = View.GONE
        }
    }

    private fun showEmptyState() {
        val trimmed = binding.etSearch.text?.toString()?.trim().orEmpty()
        binding.tvEmpty.visibility = View.VISIBLE
        binding.tvEmpty.text = if (trimmed.isBlank()) {
            "Aucun médicament trouvé. Veuillez effectuer une recherche."
        } else {
            "Aucun médicament trouvé pour \"$trimmed\". Essayez un autre terme."
        }
    }

    private fun scheduleSearch(query: String, immediate: Boolean = false) {
        searchJob?.cancel()
        searchJob = lifecycleScope.launch {
            if (!immediate) delay(300)
            viewModel.searchMedicaments(query.trim(), 50)
        }
    }

    companion object {
        const val EXTRA_TARGET_USER_ID = "EXTRA_TARGET_USER_ID"
        const val EXTRA_PREFILL_START_DATE_MILLIS = "EXTRA_PREFILL_START_DATE_MILLIS"
        const val EXTRA_PREFILL_INSTRUCTIONS = "EXTRA_PREFILL_INSTRUCTIONS"
        const val EXTRA_NAVIGATE_TO_HOME_AFTER = "EXTRA_NAVIGATE_TO_HOME_AFTER"
    }
}
