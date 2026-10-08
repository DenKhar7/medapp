package iut.butinfo3.app_mobile.model.repository

import iut.butinfo3.app_mobile.model.dao.UserDao
import iut.butinfo3.app_mobile.model.entity.User
import kotlinx.coroutines.flow.Flow

class UserRepository(private val userDao: UserDao) {

    suspend fun getUserByName(username: String): User? {
        return userDao.getUserByName(username)
    }
    fun getUserById(uid: Int): Flow<User> {
        return userDao.getUserById(uid)
    }
    /**
     * Permet de récupérer tous les child associé à un compte parent, sans récupérer le compte parent.
     */
    fun getLinkedUsers(parentId: Int): Flow<List<User>>{
        return userDao.getLinkedUsers(parentId)
    }
    suspend fun updateUser(updatedUser : User){
        return userDao.update(updatedUser)
    }
    /**
     * Permet de récupérer tous les child associé à un compte parent, en récupérant le compte parent.
     * Le compte parent est placé à la première position de la liste renvoyé.
     */
    fun getAllMembers(parentId: Int): Flow<List<User>>{
        return userDao.getAllMembers(parentId)
    }

    /**
     * Utilisé dans la page d'ajout d'un profil,
     * Ajout d'un compte avec une relation qui le lie avec son parent,
     * l'utilisateur créer ne pourra pas encore se connecter puisque qu'il n'a ni identifiant, ni mot de passe.
     */
    suspend fun createLinkedProfile(
        newProfile: User,
        parentId: Int,
        relation: String?
    ){
        return userDao.createLinkedProfile(
            newProfile,
            parentId,
            relation
        )
    }
}