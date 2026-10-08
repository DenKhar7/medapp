package iut.butinfo3.app_mobile.view_model

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import iut.butinfo3.app_mobile.model.entity.User
import iut.butinfo3.app_mobile.model.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Date

/**
 * ViewModel dédié à la création de profils secondaires (Enfants, Parents âgés...).
 *
 * @property userRepository Le dépôt pour accéder à la BDD.
 * @property currentUserId L'ID du parent qui crée ce profil (passé via la Factory).
 */
class AddProfileViewModel(
    val userRepository: UserRepository,
    private val currentUserId: Int
) : ViewModel() {
    private val _addProfileState = MutableStateFlow<AddProfileState>(AddProfileState.Idle)
    val addProfileState = _addProfileState.asStateFlow()

    /**
     * Crée un nouveau profil "Fantôme" et le lie au compte parent.
     *
     * @param nom Le nom affiché du profil.
     * @param relation Le lien de parenté (ex: "Fils", "Mère").
     */
    //TODO: Pour l'instant la relation n'est pas utilisé, a voir pour de futurs améliorations
    fun createProfile(nom: String, relation: String) {
        viewModelScope.launch {
            _addProfileState.value = AddProfileState.Loading
            try {
                val ghostUser = User(
                    username = nom,
                    createdAt = Date(),
                    isActive = false,
                )
                userRepository.createLinkedProfile(ghostUser, currentUserId, relation)
                _addProfileState.value = AddProfileState.Success

            } catch (e: Exception) {
                _addProfileState.value = AddProfileState.Error(e.message ?: "Erreur inconnue")            }
        }
    }
}

/**
 * Les différents états possibles de l'écran d'ajout.
 * L'utilisation d'une Sealed Class force à traiter tous les cas dans le "when" de l'Activity.
 */
sealed class AddProfileState {
    object Success : AddProfileState()
    object Loading : AddProfileState()
    object Idle : AddProfileState()
    data class Error(val message: String) : AddProfileState()
}