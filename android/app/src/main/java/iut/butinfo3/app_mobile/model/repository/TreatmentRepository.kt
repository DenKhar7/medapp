package iut.butinfo3.app_mobile.model.repository

import iut.butinfo3.app_mobile.model.DailyIntake
import iut.butinfo3.app_mobile.model.dao.TreatmentDao
import iut.butinfo3.app_mobile.model.entity.RecurrenceUnit
import iut.butinfo3.app_mobile.model.entity.IntakeStatus
import iut.butinfo3.app_mobile.model.entity.TreatmentHistory
import iut.butinfo3.app_mobile.model.entity.TreatmentReminder
import iut.butinfo3.app_mobile.model.entity.TreatmentWithMedicament
import iut.butinfo3.app_mobile.model.entity.UserTreatment
import iut.butinfo3.app_mobile.utils.RecurrenceUtils
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import java.util.Calendar
import java.util.Date

class TreatmentRepository(private val dao: TreatmentDao) {
    /**
     * Sauvegarde un traitement avec ses rappels.
     *
     * @return Liste des rappels avec leurs IDs auto-générés, utiles pour programmer les alarmes
     */
    suspend fun saveTreatmentWithReminders(
        treatment: UserTreatment,
        reminders: List<TreatmentReminder>
    ): List<TreatmentReminder> {
        return dao.addTreatmentWithReminders(treatment, reminders)
    }

    fun getUserTreatments(userId: Int) : Flow<List<TreatmentWithMedicament>> {
        return dao.getUserTreatments(userId)
    }

    suspend fun deleteTreatment(treatment: UserTreatment) {
        dao.deleteTreatment(treatment)
    }

    /**
     * Met à jour un traitement avec ses rappels.
     * Les anciens rappels sont supprimés et les nouveaux sont créés.
     *
     * @return Liste des rappels avec leurs IDs auto-générés, utiles pour reprogrammer les alarmes
     */
    suspend fun updateTreatmentWithReminders(treatment: UserTreatment, reminders: List<TreatmentReminder>): List<TreatmentReminder> {
        val remindersWithId = reminders.map {
            it.copy(id = 0, treatmentId = treatment.id)
        }
        return dao.updateTreatmentWithReminders(treatment, remindersWithId)
    }

    suspend fun getTreatmentById(id:Long): UserTreatment?{
        return dao.getTreatmentById(id)
    }

    suspend fun getRemindersForTreatment(treatmentId:Long): List<TreatmentReminder>{
        return dao.getRemindersForTreatment(treatmentId)
    }


    /**
     * Construit le flux temps réel des médicaments actifs.
     *
     * Cette fonction réalise une "jointure" réactive entre deux tables :
     * 1. La liste théorique des rappels (ce qu'il faut prendre).
     * 2. L'historique des actions (ce qui a été coché aujourd'hui).
     *
     * Grâce à l'opérateur [combine], la liste se recalcule et met à jour l'UI automatiquement
     * si l'une des deux sources change (ex: on ajoute un traitement OU on coche une case).
     *
     * @param userId L'identifiant de l'utilisateur affiché.
     * @return Un Flow contenant la liste des [DailyIntake] avec leur état (isTaken Vrai/Faux).
     */
    fun getActiveRemindersFlow(userId: Int): Flow<List<DailyIntake>> {
        val todayMidnight = getStartOfDay()
        val endOfToday = getEndOfDay()

        return combine(
            dao.getActiveReminders(userId,endOfToday),
            dao.getHistoryForDate(todayMidnight)
        ) { reminders, history ->
            reminders.map { reminder ->
                val isTaken = history.any { it.reminderId == reminder.reminderId }
                DailyIntake(
                    reminderId = reminder.reminderId,
                    medicamentName = reminder.drugName,
                    medicamentCip = reminder.drugCip,
                    dosage = reminder.doseQuantity,
                    time = reminder.timeOfDay,
                    isTaken = isTaken,
                    treatmentId = reminder.treatmentId,
                    recurrenceUnit = RecurrenceUnit.fromString(reminder.recurrenceUnit),
                    recurrenceInterval = reminder.recurrenceInterval,
                    startDate = reminder.startDate
                )
            }
        }
    }

    /**
     * Ajoute le médicament pris dans l'historique des prises (si on décoche le médoc, la prise est retiré)
     */
    suspend fun setMedicamentTaken(intake: DailyIntake, isTaken: Boolean) {
        val todayDate = getStartOfDay()
        if (isTaken) {
            dao.insertHistory(
                TreatmentHistory(
                    reminderId = intake.reminderId,
                    dateTaken = todayDate,
                    takenAt = Date(),
                    status = IntakeStatus.TAKEN
                )
            )
        } else {
            dao.deleteHistory(intake.reminderId, todayDate)
        }
    }


    
    /**
     * Récupère le statut des médicaments pour une date donnée de manière synchrone (pour une date passée).
     * @param userId L'identifiant de l'utilisateur
     * @param date La date pour laquelle on veut connaître le statut
     * @return Une paire (total, pris) représentant le nombre total de médicaments prévus et le nombre pris
     */
    suspend fun getMedicationStatusForDateSync(userId: Int, date: Date): Pair<Int, Int> {
        val startOfDay = getStartOfDayForDate(date)
        val endOfDay = getEndOfDayForDate(date)

        // Récupérer les rappels et l'historique de manière synchrone
        val allReminders = dao.getActiveReminders(userId, endOfDay).first()
        val history = dao.getHistoryForDate(startOfDay).first()

        // Filtrer par récurrence : ne garder que les rappels dus à cette date
        val reminders = allReminders.filter { reminder ->
            RecurrenceUtils.isReminderDueOnDate(
                targetDate = date,
                treatmentStartDate = reminder.startDate,
                interval = reminder.recurrenceInterval,
                unit = RecurrenceUnit.fromString(reminder.recurrenceUnit)
            )
        }

        val total = reminders.size
        val taken = reminders.count { reminder ->
            history.any { it.reminderId == reminder.reminderId && it.status == IntakeStatus.TAKEN }
        }

        return Pair(total, taken)
    }

    /**
     * Récupère les DailyIntake pour une date arbitraire.
     * Pour les jours passés, détermine le statut (TAKEN/MISSED) via l'historique.
     * Pour les jours futurs, affiche sans statut de prise.
     */
    suspend fun getIntakesForDate(userId: Int, date: Date): List<DailyIntake> {
        val startOfDay = getStartOfDayForDate(date)
        val endOfDay = getEndOfDayForDate(date)
        val today = getStartOfDay()

        val allReminders = dao.getActiveReminders(userId, endOfDay).first()
        val history = dao.getHistoryForDate(startOfDay).first()

        val isPast = startOfDay.before(today)

        return allReminders
            .filter { reminder ->
                RecurrenceUtils.isReminderDueOnDate(
                    targetDate = date,
                    treatmentStartDate = reminder.startDate,
                    interval = reminder.recurrenceInterval,
                    unit = RecurrenceUnit.fromString(reminder.recurrenceUnit)
                )
            }
            .map { reminder ->
                val historyEntry = history.find { it.reminderId == reminder.reminderId }
                val isTaken = historyEntry?.status == IntakeStatus.TAKEN
                val status = when {
                    historyEntry != null -> historyEntry.status
                    isPast -> IntakeStatus.MISSED
                    else -> null
                }
                DailyIntake(
                    reminderId = reminder.reminderId,
                    medicamentName = reminder.drugName,
                    medicamentCip = reminder.drugCip,
                    dosage = reminder.doseQuantity,
                    time = reminder.timeOfDay,
                    isTaken = isTaken,
                    treatmentId = reminder.treatmentId,
                    recurrenceUnit = RecurrenceUnit.fromString(reminder.recurrenceUnit),
                    recurrenceInterval = reminder.recurrenceInterval,
                    startDate = reminder.startDate,
                    intakeStatus = status
                )
            }
    }

    /**
     * Ces fonctions sont utilisé pour éviter des soucis en fonction de l'heure de la prise
     */
    private fun getStartOfDay(): Date {
        val calendar = Calendar.getInstance()
        calendar[Calendar.HOUR_OF_DAY]= 0
        calendar[Calendar.MINUTE] = 0
        calendar[Calendar.SECOND] = 0
        calendar[Calendar.MILLISECOND] = 0
        return calendar.time
    }
    
    private fun getStartOfDayForDate(date: Date): Date {
        val calendar = Calendar.getInstance()
        calendar.time = date
        calendar[Calendar.HOUR_OF_DAY] = 0
        calendar[Calendar.MINUTE] = 0
        calendar[Calendar.SECOND] = 0
        calendar[Calendar.MILLISECOND] = 0
        return calendar.time
    }
    
    private fun getEndOfDay(): Date {
        val calendar = Calendar.getInstance()
        calendar[Calendar.HOUR_OF_DAY] = 23
        calendar[Calendar.MINUTE] = 59
        calendar[Calendar.SECOND] = 59
        calendar[Calendar.MILLISECOND] = 999
        return calendar.time
    }
    
    private fun getEndOfDayForDate(date: Date): Date {
        val calendar = Calendar.getInstance()
        calendar.time = date
        calendar[Calendar.HOUR_OF_DAY] = 23
        calendar[Calendar.MINUTE] = 59
        calendar[Calendar.SECOND] = 59
        calendar[Calendar.MILLISECOND] = 999
        return calendar.time
    }
}