package medicapp.server.infrastructure.jobs.launcher

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Timer
import kotlin.concurrent.timerTask

/**
 * This function will schedule a task at the given time
 * and execute it every day at that particular time.
 *
 * @param hour: hours will be in 24 hr format
 * @param minute: minutes
 * @param second: seconds
 * @param scope: CoroutineScope to launch the task
 * @param dispatcher: CoroutineDispatcher for the task execution (default: Dispatchers.IO)
 * @param task: suspending function to execute
 **/
fun taskScheduler(
    hour: Int,
    minute: Int,
    second: Int,
    scope: CoroutineScope,
    dispatcher: CoroutineDispatcher = Dispatchers.IO,
    task: suspend () -> Unit
) {
    val currentTime = Calendar.getInstance()

    val schedulerTime = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, hour)
        set(Calendar.MINUTE, minute)
        set(Calendar.SECOND, second)
        set(Calendar.MILLISECOND, 0)
    }

    var initialDelay = schedulerTime.timeInMillis - currentTime.timeInMillis
    if (initialDelay < 0) {
        schedulerTime.add(Calendar.DAY_OF_MONTH, 1)
        initialDelay = schedulerTime.timeInMillis - currentTime.timeInMillis
    }

    val regularRunInterval = 86_400_000L // 24h

    Timer("refUpdateJob-timer", /*isDaemon=*/true).scheduleAtFixedRate(
        timerTask {
            scope.launch(dispatcher) { task() }
        },
        initialDelay,
        regularRunInterval
    )
}