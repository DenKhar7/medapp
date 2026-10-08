package iut.butinfo3.app_mobile.model.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import iut.butinfo3.app_mobile.model.entity.MedicamentPresDetail
import iut.butinfo3.app_mobile.model.entity.MedicamentSpeResume

/**
 * Dao utilisé pour tous les appels concernant les entités MedicamentPresDetail, MedicamentSpeResume.
 */
@Dao
interface MedicamentDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(medicament: MedicamentPresDetail)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(medicament: MedicamentSpeResume)

    @Query("SELECT * FROM medicament_presentation WHERE cip13 = :cip LIMIT 1")
    suspend fun getByCip(cip: String): MedicamentPresDetail?

    @Query("SELECT * FROM medicament_specialite WHERE cis = :cis LIMIT 1")
    suspend fun getSpeByCis(cis: Int): MedicamentSpeResume?

    @Query("SELECT * FROM medicament_presentation WHERE cis = :cis")
    suspend fun getPresentationsByCis(cis: Int): List<MedicamentPresDetail>

    @Query("SELECT * FROM medicament_specialite ORDER BY nom LIMIT :limit")
    suspend fun getTopMedicaments(limit: Int): List<MedicamentSpeResume>

    @Query("SELECT * FROM medicament_specialite WHERE nom LIKE '%' || :searchTerm || '%' ORDER BY nom LIMIT :limit")
    suspend fun searchByLabel(searchTerm: String, limit: Int): List<MedicamentSpeResume>
}