package iut.butinfo3.app_mobile.view_model

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import iut.butinfo3.app_mobile.model.repository.AuthRepository
import iut.butinfo3.app_mobile.model.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

/**
 * ViewModel pour l'écran de démarrage (Splash).
 * Vérifie si une session utilisateur valide existe et redirige en conséquence.
 */
class SplashViewModel(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    private val _sessionState = MutableStateFlow<SessionState>(SessionState.Checking)
    val sessionState: StateFlow<SessionState> = _sessionState.asStateFlow()

    init {
        checkSession()
    }

    /**
     * Vérifie l'état de la session :
     * - Récupère l'ID utilisateur stocké
     * - Valide que l'utilisateur existe toujours en base
     * - Vérifie que le compte est actif
     * - Clear la session si invalide
     */
    private fun checkSession() {
        viewModelScope.launch {
            val userId = authRepository.getActiveUserId()

            if (userId == null) {
                _sessionState.value = SessionState.NoSession
                return@launch
            }

            try {
                // getUserById retourne Flow<User> - on récupère la première valeur
                val user = userRepository.getUserById(userId).firstOrNull()

                when {
                    user == null || !user.isActive -> {
                        // Utilisateur supprimé ou compte désactivé
                        authRepository.clearActiveUser()
                        _sessionState.value = SessionState.InvalidSession
                    }
                    else -> {
                        // Session valide
                        _sessionState.value = SessionState.ValidSession(userId)
                    }
                }
            } catch (e: Exception) {
                // Erreur DB ou autre - fail-safe vers login
                authRepository.clearActiveUser()
                _sessionState.value = SessionState.InvalidSession
            }
        }
    }
}

/**
 * États possibles de la session au démarrage
 */
sealed class SessionState {
    /** Vérification en cours */
    object Checking : SessionState()

    /** Aucune session stockée */
    object NoSession : SessionState()

    /** Session invalide (utilisateur supprimé/désactivé ou erreur) */
    object InvalidSession : SessionState()

    /** Session valide avec l'ID utilisateur */
    data class ValidSession(val userId: Int) : SessionState()
}
