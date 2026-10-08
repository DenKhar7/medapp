package iut.butinfo3.app_mobile.view_model

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import iut.butinfo3.app_mobile.model.repository.AuthRepository
import iut.butinfo3.app_mobile.utils.ValidateText
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Locale

/**
 * ViewModel pour l'écran d'Inscription.
 *
 * Gère la validation séquentielle du formulaire d'inscription et la création du compte.
 *
 * @property repository Le dépôt d'authentification pour l'accès BDD.
 */
class RegisterViewModel(private val repository: AuthRepository) : ViewModel() {


    private val _registerState = MutableStateFlow<RegisterState>(RegisterState.Idle)

    val registerState = _registerState.asStateFlow()

    private val validate = ValidateText()


    /**
     * Tente d'inscrire un utilisateur.
     * Applique une stratégie "Fail Fast" : on s'arrête à la première erreur rencontrée.
     *
     * @param username Nom d'utilisateur souhaité.
     * @param pwd Mot de passe.
     * @param email Email.
     * @param pwdConfirm Confirmation du mot de passe.
     */
    fun onRegister(
        username: String,
        pwd: String,
        email: String,
        pwdConfirm: String
    ) {
        viewModelScope.launch {
            _registerState.value = RegisterState.Loading

            if (pwdConfirm != pwd) {
                _registerState.value = RegisterState.WrongConfirmPassword
                return@launch
            }
            if (!validate.checkNameValid(username)) {
                _registerState.value = RegisterState.BadUserName
                return@launch
            }
            if (!validate.checkEmailValid(email)) {
                _registerState.value = RegisterState.BadEmail
                return@launch
            }
            if (!validate.checkPSWValid(pwd)) {
                _registerState.value = RegisterState.BadPassword
                return@launch
            }

            try {
                repository.registerUser(
                    username,
                    email.trim().lowercase(Locale.getDefault()),
                    pwd,
                )
                _registerState.value = RegisterState.Success

            } catch (_: android.database.sqlite.SQLiteConstraintException) {
                _registerState.value = RegisterState.EmailAlreadyExists
            } catch (_: Exception) {
                _registerState.value = RegisterState.GenericError
            }
        }
    }
}

/**
 * États exhaustifs de l'écran d'inscription.
 * Permet à l'activité de savoir exactement quel message d'erreur afficher.
 */
sealed class RegisterState {
    object Idle : RegisterState()
    object Loading : RegisterState()
    object Success : RegisterState()
    object WrongConfirmPassword : RegisterState()
    object BadUserName : RegisterState()
    object GenericError : RegisterState()
    object EmailAlreadyExists : RegisterState()
    object BadEmail : RegisterState()
    object BadPassword : RegisterState()
}