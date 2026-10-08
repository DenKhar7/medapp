package iut.butinfo3.app_mobile.view

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import iut.butinfo3.app_mobile.databinding.MedicamentActivityBinding
import iut.butinfo3.app_mobile.utils.NavigationHelper
import iut.butinfo3.app_mobile.utils.ViewModelFactory
import iut.butinfo3.app_mobile.view.adapter.MedicamentAdapter
import iut.butinfo3.app_mobile.view_model.MedicamentState
import iut.butinfo3.app_mobile.view_model.MedicamentViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Écran "Médicaments" (Recherche / Scan).
 *
 * Cette activité simule le scan de boîtes de médicaments et affiche les résultats.
 * Elle sert de point d'entrée pour créer un nouveau traitement à partir d'un médicament identifié.
 */
class MedicamentActivity : AppCompatActivity() {

    private lateinit var binding: MedicamentActivityBinding
    private lateinit var navigationHelper: NavigationHelper
    private val viewModel: MedicamentViewModel by viewModels {
        ViewModelFactory(this)
    }

    private lateinit var adapter: MedicamentAdapter
    private var searchJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = MedicamentActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupRecyclerView()
        setupObservers()
        setupBottomNavigation()
        setupSearchListener()
    }

    private fun setupRecyclerView() {
        adapter = MedicamentAdapter(
            onItemClick = { item ->
                viewModel.onMedicamentClicked(item.cis)
            },
            onAddTreatment = { item ->
                val intent = Intent(this, AddTreatmentActivity::class.java)
                intent.putExtra("EXTRA_CIP", item.cip13)
                intent.putExtra("EXTRA_NAME", item.nomSpecialite)
                startActivity(intent)
            }
        )
        binding.recyclerDrugs.layoutManager = LinearLayoutManager(this)
        binding.recyclerDrugs.adapter = this.adapter
    }

    /**
     * Observe les flux de données (Flows) du ViewModel.
     * Lance 2 coroutines:
     * - Une qui va observer les médicament rechercher, pour les ajouter au listAdapter au besoin
     *
     * Une qui va observer l'état renvoyer par le viewModel
     */
    private fun setupObservers() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.searchResults.collect { list ->
                        (binding.recyclerDrugs.adapter as? MedicamentAdapter)?.submitList(list)

                    }
                }

                launch {
                    viewModel.uiState.collect { state ->
                        when (state) {
                            is MedicamentState.Loading -> {
                                binding.tvEmptyState.visibility = View.GONE
                            }

                            is MedicamentState.Empty -> {
                                binding.tvEmptyState.visibility = View.VISIBLE
                            }

                            is MedicamentState.Error -> {
                                binding.tvEmptyState.visibility = View.GONE
                                Toast.makeText(
                                    this@MedicamentActivity,
                                    state.msg,
                                    Toast.LENGTH_SHORT
                                ).show()
                            }

                            is MedicamentState.Success -> {
                                binding.tvEmptyState.visibility = View.GONE
                            }

                            is MedicamentState.Idle -> {
                                binding.tvEmptyState.visibility = View.GONE
                            }
                        }
                    }
                }
            }
        }
    }

    private fun setupSearchListener() {
        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            }
            override fun afterTextChanged(s: Editable?) {
                val query = s?.toString()?.trim().orEmpty()
                searchJob?.cancel()
                searchJob = lifecycleScope.launch {
                    delay(300)
                    viewModel.search(query)
                }
            }
        })
    }

    private fun setupBottomNavigation() {
        navigationHelper = NavigationHelper(
            binding.navHome,
            binding.navUserTreatments,
            binding.navMedicaments,
            binding.navOrdonnances,
            binding.navPathologie
        )
        navigationHelper.init(NavigationHelper.NAV_MEDICAMENTS)

        binding.navHome.setOnClickListener {
            startActivity(Intent(this, HomeActivity::class.java))
        }
        binding.navUserTreatments.setOnClickListener {
            startActivity(Intent(this, UserTreatmentsActivity::class.java))
        }
        binding.navPathologie.setOnClickListener {
            startActivity(Intent(this, PathologieActivity::class.java))
        }
        binding.navOrdonnances.setOnClickListener {
            startActivity(Intent(this, OrdonnanceActivity::class.java))
        }
    }

    override fun onResume() {
        super.onResume()
        navigationHelper.setActiveTab(NavigationHelper.NAV_MEDICAMENTS)
    }
}