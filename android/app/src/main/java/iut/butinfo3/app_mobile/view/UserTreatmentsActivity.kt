package iut.butinfo3.app_mobile.view

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import iut.butinfo3.app_mobile.databinding.UserTreatmentsActivityBinding
import iut.butinfo3.app_mobile.model.entity.TreatmentWithMedicament
import iut.butinfo3.app_mobile.utils.NavigationHelper
import iut.butinfo3.app_mobile.utils.ViewModelFactory
import iut.butinfo3.app_mobile.utils.setupProfileSelector
import iut.butinfo3.app_mobile.utils.toReadableDate
import iut.butinfo3.app_mobile.view.adapter.UserTreatmentAdapter
import iut.butinfo3.app_mobile.view_model.MainViewModel
import iut.butinfo3.app_mobile.view_model.UserTreatmentState
import iut.butinfo3.app_mobile.view_model.UserTreatmentViewModel
import kotlinx.coroutines.launch

/**
 * Écran "Mes Traitements" (L'Armoire à Pharmacie).
 *
 * Affiche la liste complète des traitements en cours pour l'utilisateur sélectionné.
 * Permet d'Ajouter, Modifier, Supprimer ou Voir les détails d'un traitement.
 *
 * Architecture :
 * - Utilise [UserTreatmentViewModel] pour gérer les données de la liste (CRUD).
 * - Utilise [MainViewModel] pour savoir QUEL utilisateur est sélectionné dans le bandeau.
 */
class UserTreatmentsActivity : AppCompatActivity() {

    private lateinit var binding: UserTreatmentsActivityBinding
    private lateinit var navigationHelper: NavigationHelper
    private lateinit var adapter: UserTreatmentAdapter
    private val viewModel: UserTreatmentViewModel by viewModels { ViewModelFactory(this) }
    private val mainViewModel: MainViewModel by viewModels { ViewModelFactory(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = UserTreatmentsActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupRecyclerView()
        setupBottomNavigation()
        setupHeaderAndIntent()
        setupObservers()

        binding.fabAddTreatment.setOnClickListener {
            startActivity(Intent(this, MedicamentActivity::class.java))
        }
    }

    private fun setupRecyclerView() {
        adapter = UserTreatmentAdapter(
            onDetailClick = { item -> showDetailDialog(item) },
            onEditClick = { item -> navigateToEdit(item) },
            onDeleteClick = { item -> showDeleteConfirmDialog(item) }
        )
        binding.recyclerUserTreatments.layoutManager = LinearLayoutManager(this)
        binding.recyclerUserTreatments.adapter = adapter
    }
    /**
     * Observe les flux de données (Flows) du ViewModel.
     * Lance 2 coroutines:
     * - Une qui va observer les traitements de l'utilisateur, pour les ajouter/supprimer en fonction de l'utilisateur sélectionné
     *
     * Une qui va observer l'état renvoyer par le viewModel
     */
    private fun setupObservers() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {

                launch {
                    viewModel.treatments.collect { list ->
                        adapter.submitList(list)
                    }
                }
                launch {
                    viewModel.uiState.collect { state ->
                        when(state) {
                            is UserTreatmentState.DeleteSuccess -> {
                                Toast.makeText(this@UserTreatmentsActivity, "Traitement supprimé", Toast.LENGTH_SHORT).show()
                                viewModel.resetState()
                            }
                            is UserTreatmentState.Error -> {
                                Toast.makeText(this@UserTreatmentsActivity, state.message, Toast.LENGTH_SHORT).show()
                                viewModel.resetState()
                            }
                            else -> Unit
                        }
                    }
                }
            }
        }
    }

    /**
     * Gère le bandeau de profil.
     * Si l'activité est ouverte avec un "EXTRA_USER_ID", on change automatiquement de profil.
     */
    private fun setupHeaderAndIntent() {
        val targetUserId = intent.getIntExtra("EXTRA_USER_ID", -1)
        if (targetUserId != -1) {
            lifecycleScope.launch {
                mainViewModel.availableProfiles.collect { profiles ->
                    val targetUser = profiles.find { it.id == targetUserId }
                    if (targetUser != null) {
                        mainViewModel.selectUser(targetUser)
                    }
                }
            }
        }

        setupProfileSelector(binding.headerSelector.rvProfileSelector, mainViewModel)
    }

    private fun showDeleteConfirmDialog(item: TreatmentWithMedicament) {
        AlertDialog.Builder(this)
            .setTitle("Supprimer ce traitement ?")
            .setMessage("Voulez-vous vraiment supprimer ${item.medicament.label} ?")
            .setPositiveButton("Supprimer") { _, _ ->
                viewModel.deleteTreatment(item)
            }
            .setNegativeButton("Annuler", null)
            .show()
    }

    private fun showDetailDialog(item: TreatmentWithMedicament) {
        val sb = StringBuilder()
        sb.append("Substance : ${item.medicament.libelleAtc ?: "N/A"}\n")
        sb.append("Laboratoire : ${item.medicament.nomOrganisation ?: "N/A"}\n\n")

        sb.append("Début : ${item.treatment.startDate.time.toReadableDate()}\n")

        val fin = item.treatment.endDate?.time?.toReadableDate() ?: "Indéterminée"
        sb.append("Fin : $fin\n\n")

        sb.append("Instructions :\n${item.treatment.instructions ?: "Aucune"}\n")

        AlertDialog.Builder(this)
            .setTitle(item.medicament.label)
            .setMessage(sb.toString())
            .setPositiveButton("Fermer", null)
            .show()
    }

    private fun navigateToEdit(item: TreatmentWithMedicament) {
        val intent = Intent(this, AddTreatmentActivity::class.java)
        intent.putExtra("EXTRA_TREATMENT_ID", item.treatment.id.toLong())
        intent.putExtra("EXTRA_CIP", item.medicament.cip13)
        intent.putExtra("EXTRA_NAME", item.medicament.label)
        startActivity(intent)
    }

    private fun setupBottomNavigation() {
        navigationHelper = NavigationHelper(
            binding.navHome,
            binding.navUserTreatments,
            binding.navMedicaments,
            binding.navOrdonnances,
            binding.navPathologie
        )
        navigationHelper.init(NavigationHelper.NAV_SETTINGS)

        binding.navHome.setOnClickListener { startActivity(Intent(this, HomeActivity::class.java)) }
        binding.navMedicaments.setOnClickListener { startActivity(Intent(this, MedicamentActivity::class.java)) }
        binding.navPathologie.setOnClickListener { startActivity(Intent(this, PathologieActivity::class.java)) }
        binding.navOrdonnances.setOnClickListener { startActivity(Intent(this, OrdonnanceActivity::class.java)) }
    }

    override fun onResume() {
        super.onResume()
        navigationHelper.setActiveTab(NavigationHelper.NAV_SETTINGS)
    }
}