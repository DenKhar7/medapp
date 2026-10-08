package iut.butinfo3.app_mobile.model.repository

import iut.butinfo3.app_mobile.model.entity.User
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Objet utilisé pour stocker les information de session,
 * stocke qui est l'utilisateur actif pour certaine page uniquement.
 * Permet de partager les données entre différentes activités
 *
 * Ici l "object" permet de n'instancier ce repository qu'une seule fois dans la vie de l'application (Singleton)
 * pour ne pas perdre les données en changeant d'activité.
 */
object SessionRepository {
    private val _selectedUser = MutableStateFlow<User?>(null)
    val selectedUser = _selectedUser.asStateFlow()

    fun selectUser(user: User) {
        _selectedUser.value = user
    }
}