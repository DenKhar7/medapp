package iut.butinfo3.app_mobile.view

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import iut.butinfo3.app_mobile.databinding.RegisterActivityBinding
import iut.butinfo3.app_mobile.utils.ViewModelFactory
import iut.butinfo3.app_mobile.view_model.RegisterState
import iut.butinfo3.app_mobile.view_model.RegisterViewModel
import kotlinx.coroutines.launch

/**
 * Écran d'Inscription (Création de compte).
 *
 * Cette activité gère le formulaire d'enregistrement d'un nouvel utilisateur.
 * Elle délègue toute la logique de validation et d'insertion BDD au [RegisterViewModel].
 *
 * Fonctionnement Réactif :
 * L'activité observe le [RegisterState] émis par le ViewModel et met à jour l'interface en conséquence
 * (Affichage d'erreur, animation de chargement, redirection).
 */
class RegisterActivity : AppCompatActivity() {

    private val viewModel: RegisterViewModel by viewModels {
        ViewModelFactory(this)
    }

    private lateinit var binding: RegisterActivityBinding
    private val failedInscription = "Échec d'inscription"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = RegisterActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupObservers()
        setupListeners()
    }

    /**
     * Configure l'observation des états (StateFlow).
     * L'UI réagit aux décisions du ViewModel.
     */
    private fun setupObservers() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.registerState.collect { state ->
                    binding.passwordErrorText.visibility = View.GONE
                    binding.registerButton.isEnabled = true

                    when (state) {
                        is RegisterState.Loading -> {
                            binding.registerButton.isEnabled = false
                            binding.registerButton.text = "Inscription..."
                        }
                        is RegisterState.Success -> {
                            Toast.makeText(this@RegisterActivity, "Compte créé !", Toast.LENGTH_SHORT).show()
                            val intent = Intent(this@RegisterActivity, ConnectionActivity::class.java)
                            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            startActivity(intent)
                        }
                        is RegisterState.WrongConfirmPassword -> {
                            showAlertDialog("Le mot de passe de confirmation est différent du mot de passe entré")
                            resetButtonText()
                        }
                        is RegisterState.BadUserName -> {
                            showRegisterError("Le nom d'utilisateur")
                            resetButtonText()
                        }
                        is RegisterState.EmailAlreadyExists -> {
                            showNameErrorAlreadyExist()
                            resetButtonText()
                        }
                        is RegisterState.BadEmail -> {
                            showRegisterError("L'email")
                            resetButtonText()
                        }
                        is RegisterState.BadPassword -> {
                            binding.passwordErrorText.visibility = View.VISIBLE
                            resetButtonText()
                        }
                        is RegisterState.GenericError -> {
                            showRegisterUnknownError()
                            resetButtonText()
                        }
                        else -> {
                            resetButtonText()
                        }
                    }
                }
            }
        }
    }

    private fun setupListeners() {
        binding.registerButton.setOnClickListener {
            val username = binding.nameEditText.text.toString().trim()
            val email = binding.emailEditText.text.toString().trim()
            val password = binding.passwordEditText.text.toString().trim()
            val pwdConfirm = binding.confirmPasswordEditText.text.toString().trim()

            if (username.isEmpty() || password.isEmpty() || email.isEmpty()) {
                Toast.makeText(this, "Veuillez remplir tous les champs", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            viewModel.onRegister(username, password, email, pwdConfirm)
        }

        binding.connectionRegisterText.setOnClickListener {
            finish()
        }
    }

    private fun resetButtonText() {
        binding.registerButton.text = "    S'inscrire !"
        binding.registerButton.isEnabled = true
    }

    private fun showNameErrorAlreadyExist() {
        showAlertDialog("Le nom d'utilisateur ou l'email existe déjà. Veuillez réessayer.")
    }

    private fun showRegisterError(value: String) {
        showAlertDialog("$value est incorrect. Veuillez vérifier le format.")
    }

    private fun showRegisterUnknownError() {
        showAlertDialog("Une erreur technique est survenue. Veuillez réessayer plus tard.")
    }

    private fun showAlertDialog(message: String) {
        AlertDialog.Builder(this)
            .setTitle(failedInscription)
            .setMessage(message)
            .setIcon(android.R.drawable.ic_dialog_alert)
            .setPositiveButton("OK") { dialog, _ -> dialog.dismiss() }
            .setCancelable(true)
            .show()
    }
}

