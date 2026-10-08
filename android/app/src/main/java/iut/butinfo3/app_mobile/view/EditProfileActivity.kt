package iut.butinfo3.app_mobile.view

import android.app.DatePickerDialog
import android.os.Bundle
import android.widget.EditText
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import iut.butinfo3.app_mobile.databinding.EditProfileActivityBinding
import iut.butinfo3.app_mobile.model.entity.User
import iut.butinfo3.app_mobile.utils.ViewModelFactory
import iut.butinfo3.app_mobile.utils.toReadableDate
import iut.butinfo3.app_mobile.view_model.EditProfileViewModel
import iut.butinfo3.app_mobile.view_model.UserToEditState
import kotlinx.coroutines.launch
import java.util.Calendar

/**
 * Écran d'édition de profil.
 *
 * Gère deux types de profils :
 * 1. Le Compte Principal : Peut tout modifier, mais doit fournir son ancien mot de passe pour en changer.
 * 2. Les Profils Secondaires : Peuvent être modifiés sans ancien mot de passe (s'ils n'en avaient pas).
 *
 * Points clés :
 * - UX Sécurisée : Masque les champs sensibles par défaut.
 * - StateFlow : Utilise un flag [isDataLoaded] pour ne pas écraser la saisie utilisateur lors des rotations.
 */
class EditProfileActivity : AppCompatActivity() {

    private lateinit var binding: EditProfileActivityBinding
    private val viewModel: EditProfileViewModel by viewModels {
        ViewModelFactory(this)
    }

    private var userIdToEdit: Int = -1
    private var isDataLoaded = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = EditProfileActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)

        userIdToEdit = intent.getIntExtra("EXTRA_USER_ID", -1)
        if (userIdToEdit == -1) {
            finish()
            return
        }

        viewModel.loadUserProfile(userIdToEdit)

        setupListeners()
        setupObservers()
    }

    private fun setupListeners() {
        binding.btnCancel.setOnClickListener { finish() }

        binding.etBirthDate.setOnClickListener { showDatePicker(binding.etBirthDate) }

        binding.btnSave.setOnClickListener {

            viewModel.updateProfile(
                username = binding.etUserName.text.toString(),
                email = binding.etEmail.text.toString(),
                weightStr = binding.etWeight.text.toString(),
                birthDateMillis = binding.etBirthDate.tag as? Long,

                oldPassword = binding.etOldPassword.text.toString(),
                newPassword = binding.etNewPassword.text.toString(),
                confirmPassword = binding.etConfirmPassword.text.toString()
            )
        }
    }

    private fun setupObservers() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.userToEdit.collect { user ->
                        if (user != null && !isDataLoaded) {
                            prefillFields(user)
                            setupSecurityUI(user)
                            isDataLoaded = true
                        }
                    }
                }

                launch {
                    viewModel.updateState.collect { state ->
                        handleUpdateState(state)
                    }
                }
            }
        }
    }

    private fun prefillFields(user: User) {
        binding.etUserName.setText(user.username)
        binding.etEmail.setText(user.email)
        binding.etWeight.setText(user.weight?.toString() ?: "")

        user.birthDate?.let { date ->
            binding.etBirthDate.setText(date.time.toReadableDate())
            binding.etBirthDate.tag = date.time
        }
    }

    /**
     * Gère les erreurs spécifiques.
     * Au lieu d'un Toast générique, on affiche l'erreur sous le champ concerné (.error).
     */
    private fun handleUpdateState(state: UserToEditState) {
        when (state) {
            is UserToEditState.Loading -> {
                binding.btnSave.isEnabled = false
                binding.btnSave.text = "Sauvegarde..."
            }
            is UserToEditState.Update -> {
                Toast.makeText(this, "Profil mis à jour !", Toast.LENGTH_SHORT).show()
                finish()
            }
            is UserToEditState.Error -> {
                binding.btnSave.isEnabled = true
                binding.btnSave.text = "Enregistrer"

                when {
                    state.message.contains("correspondent pas", true) ->
                        binding.etConfirmPassword.error = state.message
                    state.message.contains("actuel", true) ->
                        binding.etOldPassword.error = state.message
                    state.message.contains("8 caractères", true) ->
                        binding.etNewPassword.error = "Format invalide"
                    else ->
                        Toast.makeText(this, state.message, Toast.LENGTH_LONG).show()
                }
            }
            else -> {
                binding.btnSave.isEnabled = true
                binding.btnSave.text = "Enregistrer"
            }
        }
    }

    /**
     * Configure l'interface de sécurité de manière dynamique.
     * Affiche ou masque le champ "Ancien mot de passe" selon le contexte.
     */
    private fun setupSecurityUI(user: User) {
        val hasPassword = user.passwordHash != null

        if (hasPassword) {
            binding.tvSecurityInfo.text = "Modifiez votre mot de passe pour sécuriser le compte."
            binding.layoutPasswordFields.isVisible = false
            binding.btnTriggerPasswordChange.isVisible = true

            binding.btnTriggerPasswordChange.setOnClickListener {
                binding.layoutPasswordFields.isVisible = true
                binding.btnTriggerPasswordChange.isVisible = false
                binding.tilOldPassword.isVisible = true
            }
        } else {
            binding.tvSecurityInfo.text = "Ce profil n'a pas d'accès. Définissez un mot de passe pour le transformer en compte autonome."
            binding.layoutPasswordFields.isVisible = true
            binding.btnTriggerPasswordChange.isVisible = false
            binding.tilOldPassword.isVisible = false
        }
    }

    private fun showDatePicker(editText: EditText) {
        val currentMillis = editText.tag as? Long ?: System.currentTimeMillis()
        val c = Calendar.getInstance().apply { timeInMillis = currentMillis }

        DatePickerDialog(this, { _, y, m, d ->
            val newCal = Calendar.getInstance().apply { set(y, m, d) }
            editText.setText(newCal.timeInMillis.toReadableDate())
            editText.tag = newCal.timeInMillis
        }, c[Calendar.YEAR], c[Calendar.MONTH], c[Calendar.DAY_OF_MONTH]).show()
    }
}