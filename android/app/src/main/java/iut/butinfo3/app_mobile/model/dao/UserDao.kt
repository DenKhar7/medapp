package iut.butinfo3.app_mobile.model.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import iut.butinfo3.app_mobile.model.entity.LinkUserUser
import iut.butinfo3.app_mobile.model.entity.User
import kotlinx.coroutines.flow.Flow
import java.sql.Timestamp

/**
 * Dao utilisé pour tous les appels concernant les entités LinkUserUser, User.
 */
@Dao
interface UserDao {

    @Query("SELECT * FROM user where id= :id")
    fun getUserById(id:Int): Flow<User>

    @Query("SELECT * FROM user")
    fun getAllUsers(): Flow<List<User>>

    @Transaction
    @Insert
    suspend fun insertUser(user: User) : Long

    @Transaction
    @Insert
    suspend fun insertLink(link: LinkUserUser)

    @Query("SELECT * FROM User WHERE username=:username")
    suspend fun getUserByName(username: String) :User?

    @Query("SELECT * FROM User WHERE email=:email")
    suspend fun getUserByEmail(email: String) :User?

    @Delete
    fun delete(user:User)

    @Update
    suspend fun update(user: User)


    /**
     * Utilisé dans la page d'ajout d'un profil,
     * Ajout d'un compte avec une relation qui le lie avec son parent,
     * l'utilisateur créer ne pourra pas encore se connecter puisque qu'il n'a ni identifiant, ni mot de passe.
     */
    @Transaction
    suspend fun createLinkedProfile(
        newProfile: User,
        parentId: Int,
        relation: String?
    ) {

        val newChildId = insertUser(newProfile)

        val link = LinkUserUser(
            parentUserId = parentId,
            childUserId = newChildId.toInt(),
            createdAt = Timestamp(System.currentTimeMillis()),
            relationType = relation
        )

        insertLink(link)
    }

    /**
     * Permet de récupérer tous les child associé à un compte parent, sans récupérer le compte parent.
     */
    @Transaction
    @Query("""
        SELECT u.*
        FROM user u
        INNER JOIN link_user_user l ON u.id = l.child_user_id
        WHERE l.parent_user_id = :parentId
    """)
    fun getLinkedUsers(parentId: Int): Flow<List<User>>

    /**
     * Permet de récupérer tous les child associé à un compte parent, en récupérant le compte parent.
     * Le compte parent est placé à la première position de la liste renvoyé.
     */
    @Transaction
    @Query("""
    SELECT * FROM user WHERE id = :parentId
    UNION
    SELECT u.*
    FROM user u
    INNER JOIN link_user_user l ON u.id = l.child_user_id
    WHERE l.parent_user_id = :parentId
""")
    fun getAllMembers(parentId: Int): Flow<List<User>>
}