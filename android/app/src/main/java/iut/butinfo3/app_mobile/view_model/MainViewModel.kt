package iut.butinfo3.app_mobile.view_model

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import iut.butinfo3.app_mobile.model.entity.User
import iut.butinfo3.app_mobile.model.repository.AuthRepository
import iut.butinfo3.app_mobile.model.repository.SessionRepository
import iut.butinfo3.app_mobile.model.repository.UserRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

/**
 * ViewModel Global (Scope Activité).
 *
 * Il gère le contexte de la session utilisateur :
 * 1. Liste les profils disponibles pour le sélecteur (Header).
 * 2. Gère quel profil est "Actif" (celui dont on voit les données).
 *
 * Il est utilisé par le [app_mobile.view.HomeActivity] et potentiellement
 * partagé avec d'autres fragments qui ont besoin de savoir "Qui est connecté ?".
 */
class MainViewModel(
    private val userRepository: UserRepository,
    private val authRepository: AuthRepository,
    private val sessionRepository: SessionRepository,
) : ViewModel() {

    val parentUserId = authRepository.getActiveUserId()!!

    val selectedUser = sessionRepository.selectedUser

    val availableProfiles: Flow<List<User>> = userRepository.getAllMembers(authRepository.getActiveUserId()!!)

    /**
     * Change le profil actif.
     * Cette action va automatiquement déclencher la mise à jour des autres ViewModels
     * (comme HomeViewModel) qui observent le SessionRepository.
     */
    fun selectUser(user: User) {
        sessionRepository.selectUser(user)
    }

    init {
        viewModelScope.launch {
            userRepository.getAllMembers(authRepository.getActiveUserId()!!).collect { profiles ->
                if (sessionRepository.selectedUser.value == null && profiles.isNotEmpty()) {
                    sessionRepository.selectUser(profiles.first())
                }
            }
        }
    }
}
