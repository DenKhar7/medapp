package iut.butinfo3.app_mobile.reminder

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import iut.butinfo3.app_mobile.model.entity.TreatmentReminder
import java.util.Calendar

/**
 * Gestionnaire des rappels de médicaments via AlarmManager.
 * Programme et annule les alarmes pour les notifications de prise.
 */
class ReminderManager(private val context: Context) {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    /**
     * Programme un rappel pour une prise de médicament.
     *
     * @param reminder Le rappel contenant l'heure et les informations de dose
     * @param drugName Le nom du médicament
     * @param userName Le nom de l'utilisateur
     * @param treatmentId L'ID du traitement associé
     * @return true si le rappel a été programmé avec succès, false sinon
     */
    fun scheduleReminder(
        reminder: TreatmentReminder,
        drugName: String,
        userName: String,
        treatmentId: Int = reminder.treatmentId
    ): Boolean {
        // Vérification que l'ID est valide (doit être > 0 après insertion en BDD)
        if (reminder.id <= 0L) {
            Log.e(TAG, "Cannot schedule reminder with invalid id: ${reminder.id}")
            return false
        }

        // Parse l'heure avec gestion des erreurs
        val (hour, minute) = parseTimeOfDay(reminder.timeOfDay) ?: run {
            Log.e(TAG, "Invalid time format: ${reminder.timeOfDay}")
            return false
        }

        // Annule l'ancien rappel s'il existe
        cancelReminder(reminder.id)

        val intent = Intent(context, ReminderBroadcastReceiver::class.java).apply {
            putExtra(ReminderBroadcastReceiver.EXTRA_DRUG_NAME, drugName)
            putExtra(ReminderBroadcastReceiver.EXTRA_DOSE_QUANTITY, reminder.doseQuantity)
            putExtra(ReminderBroadcastReceiver.EXTRA_USERNAME, userName)
            putExtra(ReminderBroadcastReceiver.EXTRA_REMINDER_ID, reminder.id)
            putExtra(ReminderBroadcastReceiver.EXTRA_TREATMENT_ID, treatmentId)
        }

        // Toujours utiliser reminder.id comme request code pour cohérence avec cancelReminder
        val requestCode = reminder.id.toInt()

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)

            // Si l'heure est déjà passée aujourd'hui, programmer pour demain
            if (before(Calendar.getInstance())) {
                add(Calendar.DATE, 1)
            }
        }

        return scheduleAlarm(pendingIntent, calendar.timeInMillis)
    }

    /**
     * Programme un rappel "snooze" (reporter) dans un délai donné.
     *
     * @param reminderId L'ID du rappel original
     * @param drugName Le nom du médicament
     * @param doseQuantity La quantité à prendre
     * @param userName Le nom de l'utilisateur
     * @param treatmentId L'ID du traitement
     * @param delayMinutes Le délai en minutes (défaut: 15)
     * @return true si programmé avec succès
     */
    fun scheduleSnooze(
        reminderId: Long,
        drugName: String,
        doseQuantity: String,
        userName: String,
        treatmentId: Int,
        delayMinutes: Int = SNOOZE_DELAY_MINUTES
    ): Boolean {
        val intent = Intent(context, ReminderBroadcastReceiver::class.java).apply {
            putExtra(ReminderBroadcastReceiver.EXTRA_DRUG_NAME, drugName)
            putExtra(ReminderBroadcastReceiver.EXTRA_DOSE_QUANTITY, doseQuantity)
            putExtra(ReminderBroadcastReceiver.EXTRA_USERNAME, userName)
            putExtra(ReminderBroadcastReceiver.EXTRA_REMINDER_ID, reminderId)
            putExtra(ReminderBroadcastReceiver.EXTRA_TREATMENT_ID, treatmentId)
        }

        // Utilise un request code différent pour le snooze pour ne pas écraser l'alarme principale
        val requestCode = "snooze_$reminderId".hashCode()

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val triggerTime = System.currentTimeMillis() + (delayMinutes * 60 * 1000L)
        return scheduleAlarm(pendingIntent, triggerTime)
    }

    /**
     * Programme une alarme avec fallback si les alarmes exactes ne sont pas autorisées.
     */
    private fun scheduleAlarm(pendingIntent: PendingIntent, triggerAtMillis: Long): Boolean {
        return try {
            if (!alarmManager.canScheduleExactAlarms()) {
                // Fallback: utilise une alarme inexacte mais qui peut réveiller l'appareil
                Log.w(TAG, "Exact alarms not allowed, using inexact alarm as fallback")
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
            } else {
                // Alarme exacte
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
            }
            true
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException scheduling alarm", e)
            false
        }
    }

    /**
     * Parse une heure au format "HH:mm" de manière sécurisée.
     *
     * @return Pair(hour, minute) ou null si le format est invalide
     */
    private fun parseTimeOfDay(timeOfDay: String): Pair<Int, Int>? {
        return try {
            val parts = timeOfDay.split(":")
            if (parts.size != 2) return null

            val hour = parts[0].toIntOrNull() ?: return null
            val minute = parts[1].toIntOrNull() ?: return null

            if (hour !in 0..23 || minute !in 0..59) return null

            Pair(hour, minute)
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing time: $timeOfDay", e)
            null
        }
    }

    /**
     * Annule un rappel programmé.
     *
     * @param reminderId L'ID du rappel à annuler
     */
    fun cancelReminder(reminderId: Long) {
        if (reminderId <= 0L) return

        val intent = Intent(context, ReminderBroadcastReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            reminderId.toInt(),
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )

        pendingIntent?.let {
            alarmManager.cancel(it)
            it.cancel()
        }
    }

    companion object {
        private const val TAG = "ReminderManager"
        const val SNOOZE_DELAY_MINUTES = 15
    }
}