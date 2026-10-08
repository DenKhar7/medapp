package iut.butinfo3.app_mobile.model.repository

import android.content.Context
import iut.butinfo3.app_mobile.model.dao.UserDao
import iut.butinfo3.app_mobile.model.entity.User
import iut.butinfo3.app_mobile.utils.SecurityUtils
import java.util.Date
import java.time.Instant

/**
 * Classe permettant de gérer tout l'aspect d'authentification et de création d'un utilisateur
 * sur les pages de connexion et d'inscription.
 * Permet aussi de récupérer l'utilisateur actif
 */
class AuthRepository(
    context: Context,
    private val userDao: UserDao,
    /** Stockage de la session ; chiffré en production, remplaçable en mémoire dans les tests. */
    private val activeUserStore: ActiveUserStore = EncryptedActiveUserStore(context)
) {

    /**
     * Classe Utils permettant de stocker les mot de passes de manière sécurisé(par un hash avec un salt créer aléatoirement)
     */
    private val securityUtils = SecurityUtils()

    /** Session locale : identifiant de l'utilisateur connecté (voir [ActiveUserStore]). */
    fun saveActiveUserId(uid: Int) = activeUserStore.save(uid)

    fun getActiveUserId(): Int? = activeUserStore.get()

    fun clearActiveUser() = activeUserStore.clear()

    /**
     * Fonction de register basique, la vérifiaction des données se fait avant l'appel au repository,
     * ici securityUtils permet de chiffrer le mot de passe entré par l'invité.
     * Créer l'utilisateur en fonction des informations reçu.
     */
    suspend fun registerUser(username: String, email: String, password: String) {
        val salt = securityUtils.generateSalt()
        val hash = securityUtils.hashPassword(password, salt)

        val newUser = User(
            username = username,
            email = email,
            passwordHash = hash,
            passwordSalt = salt,
            createdAt = Date.from(Instant.now())
        )
        userDao.insertUser(newUser)
    }

    /**
     * Fonction de login basique, renvoie null si la connexion n'a pas marché
     * ( éviter les erreurs trop précise pour la sécurité ) sino return l'utilisateur authentifié
     */
    suspend fun loginUser(email: String, passwordClear: String): User? {

        val user = userDao.getUserByEmail(email) ?: return null
        if (!user.isActive) return null

        if (user.passwordHash == null) return null
        if (user.passwordSalt == null) return null

        val isValid = securityUtils.verifyPassword(
            inputPassword = passwordClear,
            storedSalt = user.passwordSalt,
            storedHash = user.passwordHash
        )
        if (!isValid) return null

        // Empreinte à l'ancien format (ou moins d'itérations) : on la recalcule maintenant que l'on
        // connaît le mot de passe en clair, avec un nouveau sel.
        if (securityUtils.needsRehash(user.passwordHash)) {
            val newSalt = securityUtils.generateSalt()
            val upgraded = user.copy(
                passwordHash = securityUtils.hashPassword(passwordClear, newSalt),
                passwordSalt = newSalt
            )
            userDao.update(upgraded)
            return upgraded
        }

        return user
    }

}