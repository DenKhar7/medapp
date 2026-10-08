package iut.butinfo3.app_mobile.reminder

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import iut.butinfo3.app_mobile.model.DailyIntake
import iut.butinfo3.app_mobile.model.database.AppDatabase
import iut.butinfo3.app_mobile.model.entity.RecurrenceUnit
import iut.butinfo3.app_mobile.model.toTreatmentReminder
import iut.butinfo3.app_mobile.utils.RecurrenceUtils
import kotlinx.coroutines.flow.first
import java.util.Calendar
import java.util.Date

/**
 * Worker qui s'exécute quotidiennement pour programmer les rappels du jour.
 * Parcourt tous les utilisateurs et programme les alarmes pour leurs médicaments actifs.
 */
class DailyReminderWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            scheduleTodayReminders()
            Result.success()
        } catch (e: Exception) {
            Log.e("DailyReminderWorker", "Error scheduling reminders", e)
            Result.retry()
        }
    }

    /**
     * Appel getTodayRemindersForUser et récupère tous les traitements du jour de l'utilisateur.
     * Pour chaque traitement créer un rappel qui enverra une notification dans le jour.
     */
    private suspend fun scheduleTodayReminders() {
        val database = AppDatabase.getDatabase(applicationContext)
        val userDao = database.userDao()
        val reminderManager = ReminderManager(applicationContext)

        val users = userDao.getAllUsers()
        val userList = users.first()
        userList.forEach { user ->
            val todayReminders = getTodayRemindersForUser(user.id)

            todayReminders.forEach { intake ->
                val scheduled = reminderManager.scheduleReminder(
                    reminder = intake.toTreatmentReminder(),
                    drugName = intake.medicamentName,
                    userName = user.username,
                    treatmentId = intake.treatmentId
                )
                if (!scheduled) {
                    Log.w(TAG, "Failed to schedule reminder ${intake.reminderId} for ${intake.medicamentName}")
                }
            }
        }
    }

    /**
     * Récupère tous les traitements actifs de l'utilisateur et
     * les filtres avec RecurrenceUtils pour ne récupérer que les traitements du jour.
     */
    private suspend fun getTodayRemindersForUser(userId: Int): List<DailyIntake> {
        val database = AppDatabase.getDatabase(applicationContext)
        val dao = database.treatmentDao()

        val endOfToday = getEndOfDay()

        return dao.getActiveReminders(userId, endOfToday).first()
            .map { reminder ->
                DailyIntake(
                    reminderId = reminder.reminderId,
                    medicamentName = reminder.drugName,
                    medicamentCip = reminder.drugCip,
                    dosage = reminder.doseQuantity,
                    time = reminder.timeOfDay,
                    isTaken = false,
                    treatmentId = reminder.treatmentId,
                    recurrenceUnit = RecurrenceUnit.fromString(reminder.recurrenceUnit),
                    recurrenceInterval = reminder.recurrenceInterval,
                    startDate = reminder.startDate
                )
            }.filter { intake ->
                RecurrenceUtils.isReminderDueToday(
                    treatmentStartDate = intake.startDate,
                    interval = intake.recurrenceInterval,
                    unit = intake.recurrenceUnit
                )
            }
    }
    private fun getEndOfDay(): Date {
        val calendar = Calendar.getInstance()
        calendar[Calendar.HOUR_OF_DAY] = 23
        calendar[Calendar.MINUTE] = 59
        calendar[Calendar.SECOND] = 59
        calendar[Calendar.MILLISECOND] = 999
        return calendar.time
    }

    companion object {
        private const val TAG = "DailyReminderWorker"
    }
}


