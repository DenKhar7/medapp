package iut.butinfo3.app_mobile.model.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import iut.butinfo3.app_mobile.model.ReminderWithSchedule
import iut.butinfo3.app_mobile.model.entity.TreatmentHistory
import iut.butinfo3.app_mobile.model.entity.TreatmentReminder
import iut.butinfo3.app_mobile.model.entity.TreatmentWithMedicament
import iut.butinfo3.app_mobile.model.entity.UserTreatment
import iut.butinfo3.app_mobile.model.entity.IntakeStatus
import kotlinx.coroutines.flow.Flow
import java.util.Date

/**
 * Dao utilisé pour tous les appels concernant les entités UserTreatment, TreatmentWithMedicament,
 * TreatmentReminder et TreatmentHistory.
 *
 * Appel parfois les entités MedicamentPresDetail pour obtenir des information sur les médocs
 */
@Dao
interface TreatmentDao {
    @Insert
    suspend fun insertTreatment(treatment: UserTreatment): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReminders(reminders: List<TreatmentReminder>): List<Long>

    /**
     * Ajoute un traitement avec ses rappels et retourne les rappels avec leurs IDs générés.
     *
     * @return Liste des rappels avec les IDs auto-générés
     */
    @Transaction
    suspend fun addTreatmentWithReminders(treatment: UserTreatment, reminders: List<TreatmentReminder>): List<TreatmentReminder> {
        val treatmentId = insertTreatment(treatment)

        val remindersWithTreatmentId = reminders.map { it.copy(treatmentId = treatmentId.toInt()) }

        val insertedIds = insertReminders(remindersWithTreatmentId)

        // Retourne les reminders avec leurs IDs auto-générés
        return remindersWithTreatmentId.mapIndexed { index, reminder ->
            reminder.copy(id = insertedIds[index])
        }
    }

    @Transaction
    @Query("SELECT * FROM user_treatment WHERE userId = :userId ORDER BY startDate DESC")
    fun getUserTreatments(userId: Int): Flow<List<TreatmentWithMedicament>>

    @Delete
    suspend fun deleteTreatment(treatment: UserTreatment)

    @Query("DELETE FROM treatment_reminder WHERE treatmentId = :treatmentId")
    suspend fun deleteAllRemindersForTreatment(treatmentId: Int)

    @Update
    suspend fun updateTreatment(treatment: UserTreatment)

    /**
     * Met à jour un traitement avec ses rappels et retourne les rappels avec leurs IDs générés.
     *
     * @return Liste des rappels avec les IDs auto-générés
     */
    @Transaction
    suspend fun updateTreatmentWithReminders(treatment: UserTreatment, newReminders: List<TreatmentReminder>): List<TreatmentReminder> {
        updateTreatment(treatment)

        deleteAllRemindersForTreatment(treatment.id)

        val insertedIds = insertReminders(newReminders)

        return newReminders.mapIndexed { index, reminder ->
            reminder.copy(id = insertedIds[index])
        }
    }



    @Query("SELECT * FROM treatment_reminder WHERE treatmentId = :treatmentId")
    suspend fun getRemindersForTreatment(treatmentId: Long): List<TreatmentReminder>

    @Query("SELECT * FROM user_treatment WHERE id = :id")
    suspend fun getTreatmentById(id: Long): UserTreatment?


    @Query("SELECT * FROM treatment_history WHERE date_taken = :date")
    fun getHistoryForDate(date: Date): Flow<List<TreatmentHistory>>

    @Insert
    suspend fun insertHistory(history: TreatmentHistory)

    @Query("DELETE FROM treatment_history WHERE reminderId = :reminderId AND date_taken = :date")
    suspend fun deleteHistory(reminderId: Int, date: Date)

    /**
     * Récupère une liste de "ReminderWithSchedule" contenant l'id du rappel, l'heure du rappel, la quantité inscrite,
     * le nom du médoc, l'id du traitement auquel sont associés le médoc et le rappel, la date du début du traitement, la fréquence du traitement (recurrence).
     *
     * Utilisé dans la page d'accueil pour afficher les traitements du jour.
     */
    @Query("SELECT COUNT(*) FROM treatment_history WHERE reminderId = :reminderId AND date_taken = :date")
    suspend fun getHistoryCountForReminderAndDate(reminderId: Int, date: Date): Int

    @Insert
    suspend fun insertHistoryBatch(histories: List<TreatmentHistory>)

    @Query("SELECT * FROM treatment_history WHERE date_taken = :date AND status = :status")
    fun getHistoryForDateWithStatus(date: Date, status: IntakeStatus): Flow<List<TreatmentHistory>>

    @Transaction
    @Query("""
    SELECT r.id as reminderId,
           r.timeOfDay, 
           r.doseQuantity,
           r.recurrenceInterval,  
           r.recurrenceUnit,      
           m.nomSpecialite as drugName, 
           m.cip13 as drugCip, 
           t.id as treatmentId,
           t.startDate as startDate 
    FROM treatment_reminder r
    INNER JOIN user_treatment t ON r.treatmentId = t.id
    INNER JOIN medicament_presentation m ON t.cipRef = m.cip13
    WHERE t.userId = :userId
     AND t.startDate <= :todayTimestamp 
     AND (t.endDate IS NULL OR t.endDate >= :todayTimestamp)
    ORDER BY r.timeOfDay ASC
""")
    fun getActiveReminders(userId: Int, todayTimestamp: Date): Flow<List<ReminderWithSchedule>>
}