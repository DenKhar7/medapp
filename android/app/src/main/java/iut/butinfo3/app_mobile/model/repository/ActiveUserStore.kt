package iut.butinfo3.app_mobile.model.repository

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * Stockage de l'identifiant de l'utilisateur connecté (la "session" locale).
 *
 * Une interface plutôt que l'implémentation directe : les tests unitaires (Robolectric) n'ont pas de
 * Keystore Android et fournissent donc une version en mémoire.
 */
interface ActiveUserStore {
    fun save(userId: Int)
    fun get(): Int?
    fun clear()
}

/**
 * Implémentation de production : EncryptedSharedPreferences.
 *
 * La clé maître (AES256_GCM) est générée et stockée dans le Keystore du téléphone ; les clés et valeurs sont
 * chiffrées (AES256_SIV / AES256_GCM).
 */
class EncryptedActiveUserStore(context: Context) : ActiveUserStore {

    private val prefs = securePrefs(context)

    override fun save(userId: Int) = prefs.edit { putInt(KEY_ACTIVE_USER_ID, userId) }

    override fun get(): Int? {
        val id = prefs.getInt(KEY_ACTIVE_USER_ID, -1)
        return if (id != -1) id else null
    }

    override fun clear() = prefs.edit { remove(KEY_ACTIVE_USER_ID) }

    private companion object {
        const val KEY_ACTIVE_USER_ID = "active_user_id"

        private var cachedPrefs: SharedPreferences? = null
        private var cachedFor: Context? = null

        /**
         * Les préférences chiffrées (clé AES dans le Keystore) coûtent cher à créer : on les crée une seule
         * fois par contexte applicatif au lieu d'une fois par ViewModel.
         */
        @Synchronized
        fun securePrefs(context: Context): SharedPreferences {
            val appContext = context.applicationContext
            cachedPrefs?.let { if (cachedFor === appContext) return it }

            val masterKey = MasterKey.Builder(appContext)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
            return EncryptedSharedPreferences.create(
                appContext,
                "secure_prefs",
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            ).also {
                cachedPrefs = it
                cachedFor = appContext
            }
        }
    }
}
