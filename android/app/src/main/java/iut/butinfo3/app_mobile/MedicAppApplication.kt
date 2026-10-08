package iut.butinfo3.app_mobile

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import iut.butinfo3.app_mobile.reminder.EndOfDayWorker
import java.util.Calendar
import java.util.concurrent.TimeUnit

/**
 * Application class pour MedicApp.
 * Initialise les ressources globales au démarrage de l'application.
 */
class MedicAppApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        scheduleEndOfDayWorker()
    }

    /**
     * Crée le canal de notification pour les rappels de médicaments.
     * Cette méthode est appelée une seule fois au démarrage de l'application
     * plutôt qu'à chaque notification.
     */
    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            NOTIFICATION_CHANNEL_ID,
            "Rappels de médicaments",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Notifications pour les rappels de prise de médicaments"
            enableVibration(true)
            setShowBadge(true)
        }

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }

    /**
     * Planifie le Worker de fin de journée qui marque les médicaments non pris comme MISSED.
     * S'exécute quotidiennement à 23h55 avec un délai initial calculé.
     */
    private fun scheduleEndOfDayWorker() {
        val now = Calendar.getInstance()
        val target = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 55)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        // Si 23h55 est déjà passé aujourd'hui, planifier pour demain
        if (now.after(target)) {
            target.add(Calendar.DAY_OF_MONTH, 1)
        }

        val initialDelayMillis = target.timeInMillis - now.timeInMillis

        val workRequest = PeriodicWorkRequestBuilder<EndOfDayWorker>(1, TimeUnit.DAYS)
            .setInitialDelay(initialDelayMillis, TimeUnit.MILLISECONDS)
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            EndOfDayWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            workRequest
        )
    }

    companion object {
        const val NOTIFICATION_CHANNEL_ID = "MEDICATION_REMINDERS"
    }
}
