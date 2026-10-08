package iut.butinfo3.app_mobile.view_model

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import iut.butinfo3.app_mobile.model.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Locale

/**
* ViewModel pour l'écran de connexion.
*
* Il gère l'authentification de l'utilisateur.
*/
class ConnectionViewModel(private val repository: AuthRepository) : ViewModel() {

    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Idle)
     val connectionState = _connectionState.asStateFlow()

    /**
     * Méthode principale : Valide la connexion
     *
     * @param email L'email entré par l'invité
     * @param pwd Password: Le mot de passe entré par l'invité
     */
    fun onConnection(email: String, pwd: String) {
        viewModelScope.launch {
            _connectionState.value= ConnectionState.Loading
            val user = repository.loginUser(email.lowercase(Locale.FRENCH),pwd)
            if (user==null){
                _connectionState.value = ConnectionState.LoginFailed
            }else {
                repository.saveActiveUserId(user.id)
                _connectionState.value = ConnectionState.Success
            }
        }
    }
}
/**
 * États possibles de l'écran (pour gérer les différent états de la connexion).
 */
sealed class ConnectionState {
    object Success : ConnectionState()
    object LoginFailed : ConnectionState()
    object Idle : ConnectionState()
    object Loading : ConnectionState()
}