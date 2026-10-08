package iut.butinfo3.app_mobile.view

import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import iut.butinfo3.app_mobile.databinding.ConnectionActivityBinding
import iut.butinfo3.app_mobile.utils.ViewModelFactory
import iut.butinfo3.app_mobile.view_model.ConnectionState
import iut.butinfo3.app_mobile.view_model.ConnectionViewModel
import kotlinx.coroutines.launch


/**
 * Écran de Connexion (Login).
 *
 * Point d'entrée principal de l'application pour les utilisateurs existants.
 *
 * Fonctionnalités Clés :
 * - Authentification MVVM : Délègue la vérification au ViewModel.
 * - Feedback Visuel : Désactive le bouton pendant le chargement (Loading state).
 * - Navigation Sécurisée : Empêche le retour en arrière après une connexion réussie.
 */
class ConnectionActivity : AppCompatActivity() {

    private lateinit var binding: ConnectionActivityBinding

    private val viewModel: ConnectionViewModel by viewModels {
        ViewModelFactory(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ConnectionActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupObservers()
        setupListeners()
    }

    /**
     * Observe l'état de la connexion (StateFlow).
     * C'est ici qu'on met à jour l'interface graphique en fonction de ce que fait le ViewModel.
     */
    private fun setupObservers() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.connectionState.collect { state ->
                    when (state) {
                        is ConnectionState.Loading -> {
                            binding.connectionButton.isEnabled = false
                            binding.connectionButton.text = "Connexion..."
                        }
                        is ConnectionState.Success -> {
                            binding.connectionButton.isEnabled = true
                            navigateToHome()
                        }
                        is ConnectionState.LoginFailed -> {
                            binding.connectionButton.isEnabled = true
                            binding.connectionButton.text = "Se connecter"
                            showLoginFailedAlert()
                        }
                        else -> {
                            binding.connectionButton.isEnabled = true
                        }
                    }
                }
            }
        }
    }

    private fun setupListeners() {
        binding.connectionButton.setOnClickListener {
            val email = binding.emailEditText.text.toString().trim()
            val password = binding.passwordEditText.text.toString().trim()

            if (email.isEmpty() || password.isEmpty()) {
                showLoginFailedAlert("Veuillez remplir tous les champs.")
                return@setOnClickListener
            }

            viewModel.onConnection(email, password)
        }

        binding.connectionRegisterText.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }

    /**
     * Redirige vers l'accueil en nettoyant l'historique.
     */
    private fun navigateToHome() {
        val intent = Intent(this, HomeActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
    }

    private fun showLoginFailedAlert(message: String = "La connexion n'a pas fonctionné.") {
        AlertDialog.Builder(this)
            .setTitle("Échec de connexion")
            .setMessage(message)
            .setPositiveButton("OK") { dialog, _ -> dialog.dismiss() }
            .show()
    }
}