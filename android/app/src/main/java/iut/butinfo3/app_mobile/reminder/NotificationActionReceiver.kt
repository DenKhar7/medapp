package iut.butinfo3.app_mobile.reminder

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast
import iut.butinfo3.app_mobile.model.database.AppDatabase
import iut.butinfo3.app_mobile.model.entity.TreatmentHistory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Date

/**
 * BroadcastReceiver pour gérer les actions de notification (Pris / Reporter).
 *
 * - ACTION_TAKEN: Marque le médicament comme pris dans l'historique
 * - ACTION_SNOOZE: Replanifie la notification dans 15 minutes
 */
class NotificationActionReceiver : BroadcastReceiver() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent) {
        val reminderId = intent.getLongExtra(ReminderBroadcastReceiver.EXTRA_REMINDER_ID, 0L)
        val drugName = intent.getStringExtra(ReminderBroadcastReceiver.EXTRA_DRUG_NAME) ?: ""

        // Ferme la notification
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(reminderId.hashCode())

        when (intent.action) {
            ACTION_TAKEN -> handleTaken(context, reminderId, drugName, goAsync())
            ACTION_SNOOZE -> handleSnooze(context, intent, reminderId)
        }
    }

    /**
     * Marque le médicament comme pris dans l'historique.
     */
    private fun handleTaken(
        context: Context,
        reminderId: Long,
        drugName: String,
        pendingResult: BroadcastReceiver.PendingResult
    ) {
        // goAsync() : sans lui, le système peut tuer le processus dès la fin de onReceive(), avant que
        // l'écriture en base ne soit terminée, et la prise serait perdue.
        scope.launch {
            try {
                val database = AppDatabase.getDatabase(context)
                val dao = database.treatmentDao()

                val todayDate = getStartOfDay()

                dao.insertHistory(
                    TreatmentHistory(
                        reminderId = reminderId.toInt(),
                        dateTaken = todayDate,
                        takenAt = Date()
                    )
                )

                Log.i(TAG, "Medication marked as taken: $drugName (reminderId=$reminderId)")

                // Affiche un toast sur le thread principal
                CoroutineScope(Dispatchers.Main).launch {
                    Toast.makeText(context, "$drugName marqué comme pris", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error marking medication as taken", e)
            } finally {
                pendingResult.finish()
            }
        }
    }

    /**
     * Replanifie la notification dans 15 minutes.
     */
    private fun handleSnooze(context: Context, intent: Intent, reminderId: Long) {
        val drugName = intent.getStringExtra(ReminderBroadcastReceiver.EXTRA_DRUG_NAME) ?: ""
        val doseQuantity = intent.getStringExtra(ReminderBroadcastReceiver.EXTRA_DOSE_QUANTITY) ?: ""
        val userName = intent.getStringExtra(ReminderBroadcastReceiver.EXTRA_USERNAME) ?: ""
        val treatmentId = intent.getIntExtra(ReminderBroadcastReceiver.EXTRA_TREATMENT_ID, 0)

        val reminderManager = ReminderManager(context)
        val scheduled = reminderManager.scheduleSnooze(
            reminderId = reminderId,
            drugName = drugName,
            doseQuantity = doseQuantity,
            userName = userName,
            treatmentId = treatmentId
        )

        if (scheduled) {
            Log.i(TAG, "Reminder snoozed: $drugName (reminderId=$reminderId)")
            Toast.makeText(
                context,
                "Rappel reporté de ${ReminderManager.SNOOZE_DELAY_MINUTES} minutes",
                Toast.LENGTH_SHORT
            ).show()
        } else {
            Log.e(TAG, "Failed to snooze reminder: $drugName")
            Toast.makeText(context, "Erreur lors du report du rappel", Toast.LENGTH_SHORT).show()
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

    companion object {
        private const val TAG = "NotificationActionReceiver"
        const val ACTION_TAKEN = "app_mobile.ACTION_TAKEN"
        const val ACTION_SNOOZE = "app_mobile.ACTION_SNOOZE"
    }
}
