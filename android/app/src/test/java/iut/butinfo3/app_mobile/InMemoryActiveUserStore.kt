package iut.butinfo3.app_mobile

import iut.butinfo3.app_mobile.model.repository.ActiveUserStore

/**
 * Stockage de session en mémoire pour les tests unitaires : Robolectric ne fournit pas l'AndroidKeyStore
 * nécessaire à EncryptedSharedPreferences.
 */
class InMemoryActiveUserStore : ActiveUserStore {
    private var id: Int? = null

    override fun save(userId: Int) {
        id = userId
    }

    override fun get(): Int? = id

    override fun clear() {
        id = null
    }
}
