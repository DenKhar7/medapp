package iut.butinfo3.app_mobile.view_model

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import iut.butinfo3.app_mobile.model.entity.User
import iut.butinfo3.app_mobile.model.repository.AuthRepository
import iut.butinfo3.app_mobile.model.repository.UserRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel pour la gestion de l'écran "Mon Compte".
 *
 * Architecture Réactive (Flows) :
 * Ce ViewModel s'abonne aux changements de la base de données en temps réel.
 * Si les données de l'utilisateur sont modifiées ailleurs dans l'app,
 * cet écran se mettra à jour automatiquement grâce au pattern Observer.
 *
 * Stabilité :
 * Utilise une gestion manuelle des [Job] pour annuler proprement les anciennes
 * requêtes lors d'un changement d'utilisateur, sans utiliser d'API expérimentales.
 */
class AccountViewModel(
    private val repository: AuthRepository,
    private val userRepository: UserRepository,
) : ViewModel() {


    private val _user = MutableStateFlow<User?>(null)
    val user = _user.asStateFlow()

    private val _linkedUsers = MutableStateFlow<List<User>>(emptyList())
    val linkedUsers = _linkedUsers.asStateFlow()

    private var userJob: Job? = null
    private var linkedUsersJob: Job? = null

    /**
     * Charge les données du compte et démarre l'observation en temps réel.
     *
     * @param userId L'ID de l'utilisateur à afficher.
     */
    fun loadAccountData(userId: Int) {
        userJob?.cancel()
        linkedUsersJob?.cancel()

        userJob = viewModelScope.launch {
            userRepository.getUserById(userId).collect { userObj ->
                _user.value = userObj
            }
        }
        linkedUsersJob = viewModelScope.launch {
            userRepository.getLinkedUsers(userId).collect { list ->
                _linkedUsers.value = list
            }
        }
    }

    /**
     * Déconnecte l'utilisateur (vide les préférences partagées).
     */
    fun onDisconnect() {
        repository.clearActiveUser()
    }
}

