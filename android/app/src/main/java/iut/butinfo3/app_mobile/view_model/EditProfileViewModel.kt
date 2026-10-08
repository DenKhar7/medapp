package iut.butinfo3.app_mobile.view_model

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import iut.butinfo3.app_mobile.model.entity.User
import iut.butinfo3.app_mobile.model.repository.UserRepository
import iut.butinfo3.app_mobile.utils.SecurityUtils
import iut.butinfo3.app_mobile.utils.ValidateText
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Date
import java.util.Locale

/**
 * ViewModel gérant la modification de profil.
 *
 * C'est ici que se trouve la logique de sécurité pour le changement de mot de passe.
 * Il gère aussi bien les comptes complets que les profils liés (enfants).
 */
class EditProfileViewModel(private val repository : UserRepository) : ViewModel(){

    private val securityUtils = SecurityUtils()
    private val validateText = ValidateText()
    private val _userToEdit = MutableStateFlow<User?>(null)
    val userToEdit = _userToEdit.asStateFlow()

    private val _updateState = MutableStateFlow<UserToEditState>(UserToEditState.Idle)
    val updateState = _updateState.asStateFlow()

    /**
     * Charge les données de l'utilisateur à modifier.
     * S'abonne au flux (Flow) pour avoir les mises à jour en temps réel.
     */
    fun loadUserProfile(userId: Int) {
        viewModelScope.launch {
            repository.getUserById(userId).collect { user ->
                _userToEdit.value=user
            }
        }
    }
    /**
     * Tente de mettre à jour le profil.
     * Cette méthode contient une grosse logique de validation, surtout pour le mot de passe.
     *
     * @param username Nouveau nom d'utilisateur.
     * @param email Nouvel email.
     * @param oldPassword L'ancien mot de passe (Requis UNIQUEMENT si l'utilisateur en avait déjà un).
     * @param newPassword Le nouveau mot de passe souhaité (Vide si pas de changement).
     */
    fun updateProfile(
        username: String,
        email: String,
        weightStr: String,
        birthDateMillis: Long?,
        oldPassword: String,
        newPassword: String,
        confirmPassword: String
    ) {
        viewModelScope.launch {
            _updateState.value = UserToEditState.Loading

            val currentUser = _userToEdit.value
            if (currentUser == null) {
                _updateState.value = UserToEditState.Error("Utilisateur introuvable.")
                return@launch
            }

            val passwordResult = processPasswordChange(currentUser, oldPassword, newPassword, confirmPassword)
            if (passwordResult is PasswordResult.Error) {
                _updateState.value = UserToEditState.Error(passwordResult.message)
                return@launch
            }

            val (finalHash, finalSalt) = (passwordResult as PasswordResult.Success).let { it.hash to it.salt }

            val updatedUser = buildUpdatedUser(
                currentUser = currentUser,
                username = username,
                email = email,
                weightStr = weightStr,
                birthDateMillis = birthDateMillis,
                passwordHash = finalHash,
                passwordSalt = finalSalt
            )

            saveUser(updatedUser)
        }
    }

    private sealed class PasswordResult {
        data class Success(val hash: String?, val salt: String?) : PasswordResult()
        data class Error(val message: String) : PasswordResult()
    }

    private fun processPasswordChange(
        currentUser: User,
        oldPassword: String,
        newPassword: String,
        confirmPassword: String
    ): PasswordResult {
        if (newPassword.isEmpty()) {
            return PasswordResult.Success(currentUser.passwordHash, currentUser.passwordSalt)
        }

        val validationError = validateNewPassword(currentUser, oldPassword, newPassword, confirmPassword)
        if (validationError != null) {
            return PasswordResult.Error(validationError)
        }

        val newSalt = securityUtils.generateSalt()
        val newHash = securityUtils.hashPassword(newPassword, newSalt)
        return PasswordResult.Success(newHash, newSalt)
    }

    private fun validateNewPassword(
        currentUser: User,
        oldPassword: String,
        newPassword: String,
        confirmPassword: String
    ): String? {
        if (!validateText.checkPSWValid(newPassword)) {
            return "Le mot de passe doit contenir 8 caractères, 1 majuscule, 1 chiffre, 1 spécial."
        }

        if (newPassword != confirmPassword) {
            return "Les nouveaux mots de passe ne correspondent pas."
        }

        if (currentUser.passwordHash != null) {
            if (oldPassword.isEmpty()) {
                return "Veuillez entrer votre mot de passe actuel."
            }
            val calculatedHash = securityUtils.hashPassword(oldPassword, currentUser.passwordSalt!!)
            if (calculatedHash != currentUser.passwordHash) {
                return "Ancien mot de passe incorrect."
            }
        }

        return null
    }

    private fun buildUpdatedUser(
        currentUser: User,
        username: String,
        email: String,
        weightStr: String,
        birthDateMillis: Long?,
        passwordHash: String?,
        passwordSalt: String?
    ) = currentUser.copy(
        username = username.trim(),
        email = email.trim().lowercase(Locale.FRENCH),
        weight = weightStr.toIntOrNull(),
        birthDate = birthDateMillis?.let { Date(it) } ?: currentUser.birthDate,
        passwordHash = passwordHash,
        passwordSalt = passwordSalt,
        isActive = passwordHash != null
    )

    private suspend fun saveUser(user: User) {
        try {
            repository.updateUser(user)
            _userToEdit.value = user
            _updateState.value = UserToEditState.Update
        } catch (e: Exception) {
            _updateState.value = UserToEditState.Error("Erreur lors de la sauvegarde : ${e.message}")
        }
    }
}

/**
 * États de l'interface d'édition.
 */
sealed class UserToEditState(){
    object Update : UserToEditState()
    object Loading : UserToEditState()
    object Idle : UserToEditState()
    data class Error(val message: String) : UserToEditState()
}