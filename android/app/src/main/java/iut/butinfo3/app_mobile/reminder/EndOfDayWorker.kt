package iut.butinfo3.app_mobile.reminder

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import iut.butinfo3.app_mobile.model.database.AppDatabase
import iut.butinfo3.app_mobile.model.entity.IntakeStatus
import iut.butinfo3.app_mobile.model.entity.RecurrenceUnit
import iut.butinfo3.app_mobile.model.entity.TreatmentHistory
import iut.butinfo3.app_mobile.utils.RecurrenceUtils
import kotlinx.coroutines.flow.first
import java.util.Calendar
import java.util.Date

/**
 * Worker qui s'exécute en fin de journée (23h55) pour marquer comme MISSED
 * les rappels de médicaments qui n'ont pas été pris.
 */
class EndOfDayWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            markMissedMedications()
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Error marking missed medications", e)
            Result.retry()
        }
    }

    private suspend fun markMissedMedications() {
        val database = AppDatabase.getDatabase(applicationContext)
        val userDao = database.userDao()
        val treatmentDao = database.treatmentDao()

        val today = Date()
        val startOfDay = getStartOfDay()
        val endOfDay = getEndOfDay()

        val users = userDao.getAllUsers().first()

        users.forEach { user ->
            val allReminders = treatmentDao.getActiveReminders(user.id, endOfDay).first()

            // Filtrer par récurrence
            val dueReminders = allReminders.filter { reminder ->
                RecurrenceUtils.isReminderDueOnDate(
                    targetDate = today,
                    treatmentStartDate = reminder.startDate,
                    interval = reminder.recurrenceInterval,
                    unit = RecurrenceUnit.fromString(reminder.recurrenceUnit)
                )
            }

            // Trouver ceux sans entrée dans l'historique
            val missedHistories = dueReminders.mapNotNull { reminder ->
                val count = treatmentDao.getHistoryCountForReminderAndDate(
                    reminder.reminderId,
                    startOfDay
                )
                if (count == 0) {
                    TreatmentHistory(
                        reminderId = reminder.reminderId,
                        dateTaken = startOfDay,
                        takenAt = Date(),
                        status = IntakeStatus.MISSED
                    )
                } else {
                    null
                }
            }

            if (missedHistories.isNotEmpty()) {
                treatmentDao.insertHistoryBatch(missedHistories)
                Log.d(TAG, "Marked ${missedHistories.size} missed medications for user ${user.id}")
            }
        }
    }

    private fun getStartOfDay(): Date {
        val calendar = Calendar.getInstance()
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

    companion object {
        const val TAG = "EndOfDayWorker"
        const val WORK_NAME = "end_of_day_missed_check"
    }
}
