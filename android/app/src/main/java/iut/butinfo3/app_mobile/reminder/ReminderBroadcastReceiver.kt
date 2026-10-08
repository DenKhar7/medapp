package iut.butinfo3.app_mobile.reminder

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import iut.butinfo3.app_mobile.MedicAppApplication
import iut.butinfo3.app_mobile.R
import iut.butinfo3.app_mobile.view.SplashActivity

/**
 * BroadcastReceiver pour afficher les notifications de rappel de médicaments.
 * Reçoit les alarmes programmées par ReminderManager et crée les notifications.
 */
class ReminderBroadcastReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val drugName = intent.getStringExtra(EXTRA_DRUG_NAME) ?: "Médicament"
        val doseQuantity = intent.getStringExtra(EXTRA_DOSE_QUANTITY) ?: ""
        val username = intent.getStringExtra(EXTRA_USERNAME) ?: ""
        val reminderId = intent.getLongExtra(EXTRA_REMINDER_ID, 0L)
        val treatmentId = intent.getIntExtra(EXTRA_TREATMENT_ID, 0)

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Intent pour ouvrir l'app quand on clique sur la notification
        val contentIntent = Intent(context, SplashActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            reminderId.toInt(),
            contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Action "Pris" - marque le médicament comme pris
        val takenIntent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = NotificationActionReceiver.ACTION_TAKEN
            putExtra(EXTRA_REMINDER_ID, reminderId)
            putExtra(EXTRA_DRUG_NAME, drugName)
            putExtra(EXTRA_TREATMENT_ID, treatmentId)
        }
        val takenPendingIntent = PendingIntent.getBroadcast(
            context,
            "taken_$reminderId".hashCode(),
            takenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Action "Reporter" - replanifie dans 15 minutes
        val snoozeIntent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = NotificationActionReceiver.ACTION_SNOOZE
            putExtra(EXTRA_REMINDER_ID, reminderId)
            putExtra(EXTRA_DRUG_NAME, drugName)
            putExtra(EXTRA_DOSE_QUANTITY, doseQuantity)
            putExtra(EXTRA_USERNAME, username)
            putExtra(EXTRA_TREATMENT_ID, treatmentId)
        }
        val snoozePendingIntent = PendingIntent.getBroadcast(
            context,
            "snooze_$reminderId".hashCode(),
            snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Version affichée sur l'écran verrouillé : ni nom du patient ni médicament (données de santé).
        val publicVersion = NotificationCompat.Builder(context, MedicAppApplication.NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_bell)
            .setContentTitle("Rappel de prise de médicament")
            .setContentText("Déverrouillez le téléphone pour voir le détail.")
            .build()

        val notification = NotificationCompat.Builder(context, MedicAppApplication.NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_bell)
            .setContentTitle("Rappel de prise de médicament")
            .setContentText("Il est l'heure pour $username de prendre $doseQuantity $drugName.")
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setPublicVersion(publicVersion)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(contentPendingIntent)
            .setAutoCancel(true)
            .addAction(R.drawable.ic_check, "Pris", takenPendingIntent)
            .addAction(R.drawable.ic_snooze, "Reporter dans 15 min", snoozePendingIntent)
            .build()

        // Utilise reminderId pour un ID stable et unique
        notificationManager.notify(getNotificationId(reminderId), notification)
    }

    /**
     * Génère un ID de notification stable basé sur le reminderId.
     * Évite les collisions contrairement à System.currentTimeMillis().toInt().
     */
    private fun getNotificationId(reminderId: Long): Int {
        return reminderId.hashCode()
    }

    companion object {
        const val EXTRA_DRUG_NAME = "EXTRA_DRUG_NAME"
        const val EXTRA_DOSE_QUANTITY = "EXTRA_DOSE_QUANTITY"
        const val EXTRA_USERNAME = "EXTRA_USERNAME"
        const val EXTRA_REMINDER_ID = "EXTRA_REMINDER_ID"
        const val EXTRA_TREATMENT_ID = "EXTRA_TREATMENT_ID"
    }
}
