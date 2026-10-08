package iut.butinfo3.app_mobile.view

import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import iut.butinfo3.app_mobile.databinding.AddProfileActivityBinding
import iut.butinfo3.app_mobile.utils.ViewModelFactory
import iut.butinfo3.app_mobile.view_model.AddProfileState
import iut.butinfo3.app_mobile.view_model.AddProfileViewModel
import kotlinx.coroutines.launch

/**
 * Écran de création d'un profil secondaire (ex: Enfant, Conjoint).
 *
 * Cette activité est toujours lancée depuis [AccountActivity].
 * Elle nécessite un "PARENT_ID" passé via l'Intent pour lier le nouveau profil au compte principal.
 */
class AddProfileActivity : AppCompatActivity() {

    private lateinit var binding: AddProfileActivityBinding
    private val viewModel: AddProfileViewModel by viewModels {
        ViewModelFactory(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = AddProfileActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (intent.getIntExtra("PARENT_ID", -1) == -1) {
            Toast.makeText(this, "Erreur de navigation : Parent inconnu", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        setupObservers()
        setupListeners()
    }

    /**
     * Écoute les changements d'état du ViewModel (Pattern Observer).
     */
    private fun setupObservers() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.addProfileState.collect { state ->
                    when (state) {
                        is AddProfileState.Loading -> {
                            binding.btnSaveProfile.isEnabled = false
                            binding.btnSaveProfile.text = "Création en cours..."
                        }
                        is AddProfileState.Success -> {
                            Toast.makeText(this@AddProfileActivity, "Profil ajouté !", Toast.LENGTH_SHORT).show()
                            finish()
                        }
                        is AddProfileState.Error -> {
                            binding.btnSaveProfile.isEnabled = true
                            binding.btnSaveProfile.text = "Enregistrer"
                            Toast.makeText(this@AddProfileActivity, state.message, Toast.LENGTH_LONG).show()
                        }
                        else -> {
                            binding.btnSaveProfile.isEnabled = true
                            binding.btnSaveProfile.text = "Enregistrer"
                        }
                    }
                }
            }
        }
    }

    private fun setupListeners() {
        binding.btnSaveProfile.setOnClickListener {
            val nom = binding.etNomComplet.text.toString().trim()
            val relation = binding.etRelation.text.toString().trim()

            if (nom.isEmpty()) {
                binding.etNomComplet.error = "Requis"
                return@setOnClickListener
            }

            viewModel.createProfile(nom, relation)
        }
    }
}