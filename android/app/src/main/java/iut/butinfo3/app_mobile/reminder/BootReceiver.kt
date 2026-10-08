package iut.butinfo3.app_mobile.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager

/**
 * Classe utilisé pour récupérer les informations de lancement du téléphone ou de modification de l'heure.
 * Créer et initialse les notifs du jour.
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED ||
            intent.action == Intent.ACTION_TIME_CHANGED ||
            intent.action == Intent.ACTION_TIMEZONE_CHANGED) {

            val workRequest = OneTimeWorkRequestBuilder<DailyReminderWorker>().build()
            WorkManager.getInstance(context).enqueue(workRequest)
        }
    }
}