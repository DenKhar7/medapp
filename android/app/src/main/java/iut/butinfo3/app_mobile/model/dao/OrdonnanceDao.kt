package iut.butinfo3.app_mobile.model.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import iut.butinfo3.app_mobile.model.entity.Ordonnance
import kotlinx.coroutines.flow.Flow

// DAO pour gerer les operations sur les ordonnances
@Dao
interface OrdonnanceDao {
    
    // Recupere toutes les ordonnances d un utilisateur triees par date de scan decroissante
    @Query("SELECT * FROM ordonnance WHERE userId = :userId ORDER BY scan_date DESC")
    fun getOrdonnancesByUser(userId: Int): Flow<List<Ordonnance>>
    
    // Recupere une ordonnance par son id
    @Query("SELECT * FROM ordonnance WHERE id = :ordonnanceId")
    suspend fun getOrdonnanceById(ordonnanceId: Int): Ordonnance?
    
    // Insere une nouvelle ordonnance
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrdonnance(ordonnance: Ordonnance): Long
    
    // Met a jour une ordonnance existante
    @Update
    suspend fun updateOrdonnance(ordonnance: Ordonnance)
    
    // Supprime une ordonnance
    @Delete
    suspend fun deleteOrdonnance(ordonnance: Ordonnance)
    
    // Compte le nombre d ordonnances pour un utilisateur
    @Query("SELECT COUNT(*) FROM ordonnance WHERE userId = :userId")
    suspend fun countOrdonnancesByUser(userId: Int): Int
}
