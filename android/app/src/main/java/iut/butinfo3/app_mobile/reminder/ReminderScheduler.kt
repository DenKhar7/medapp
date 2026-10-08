package iut.butinfo3.app_mobile.reminder

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.Calendar
import java.util.concurrent.TimeUnit

/**
 * Classe responsable de la planification et de la gestion des rappels quotidiens.
 * Elle utilise WorkManager pour garantir que les tâches s'exécutent même si l'application est fermée.
 */
class ReminderScheduler(private val context: Context) {

    /**
     * Planifie une tâche périodique qui s'exécute une fois par jour.
     * La tâche est configurée pour démarrer au prochain 00:00 et se répéter toutes les 24 heures.
     * Si une tâche est déjà planifiée, elle est conservée sans modification.
     */
    fun scheduleDaily() {
        val constraints = Constraints.Builder()
            .setRequiresBatteryNotLow(false)
            .build()

        val dailyWorkRequest = PeriodicWorkRequestBuilder<DailyReminderWorker>(
            1, TimeUnit.DAYS
        )
            .setConstraints(constraints)
            .setInitialDelay(calculateDelayUntilMidnight(), TimeUnit.MILLISECONDS)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            "daily_reminder_scheduler",
            ExistingPeriodicWorkPolicy.KEEP,
            dailyWorkRequest
        )
    }

    /**
     * Calcule le délai nécessaire (en millisecondes) entre l'instant présent
     * et le prochain passage à minuit (00:01).
     * * @return Le délai en millisecondes jusqu'au prochain minuit.
     */
    private fun calculateDelayUntilMidnight(): Long {
        val now = Calendar.getInstance()
        val midnight = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 1)
            set(Calendar.SECOND, 0)
            add(Calendar.DATE, 1)
        }
        return midnight.timeInMillis - now.timeInMillis
    }

    /**
     * Annule la tâche de rappel quotidien précédemment programmée dans WorkManager.
     */
    fun cancelDaily() {
        WorkManager.getInstance(context)
            .cancelUniqueWork("daily_reminder_scheduler")
    }
}