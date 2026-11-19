package app.orchard.tv.weather

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch

/** Main-thread scheduling, independent of the weather provider and cached reading. */
internal class WeatherRefresh(
    private val scope: CoroutineScope,
    private val nowMs: () -> Long,
    private val read: suspend () -> Boolean,
) {
    var lastAttempt: Long? = null
        private set
    val inFlight: Boolean get() = job?.isActive == true

    private var failures = 0
    private var job: Job? = null
    private var ticker: Job? = null
    private var shown = false

    fun refreshIfStale(networkCameUp: Boolean = false) {
        if (!inFlight && (dueInMs() == 0L || (networkCameUp && failures > 0))) startReading()
    }

    /** A newly chosen location should not inherit the old location's failure backoff. */
    fun refresh() {
        failures = 0
        startReading()
    }

    fun whileShown(value: Boolean) {
        if (shown == value) return
        shown = value
        scheduleNext()
    }

    private fun startReading() {
        ticker?.cancel()
        ticker = null
        job?.cancel()
        lastAttempt = nowMs()
        job = scope.launch {
            val success = read()
            ensureActive()
            failures = if (success) 0 else (failures + 1).coerceAtMost(6)
            job = null
            scheduleNext()
        }
    }

    private fun dueInMs(): Long {
        val attempted = lastAttempt ?: return 0L
        val interval = if (failures == 0) HOUR_MS else
            (RETRY_MS shl (failures - 1)).coerceAtMost(HOUR_MS)
        return (interval - (nowMs() - attempted).coerceAtLeast(0L)).coerceAtLeast(0L)
    }

    private fun scheduleNext() {
        ticker?.cancel()
        ticker = null
        if (!shown || inFlight) return
        ticker = scope.launch {
            // Re-arm after every reading: a manual location change can shorten an hourly wait.
            delay(dueInMs() + 1_000L)
            ticker = null
            refreshIfStale()
            if (!inFlight) scheduleNext()
        }
    }

    private companion object {
        const val HOUR_MS = 60 * 60 * 1000L
        const val RETRY_MS = 3 * 60 * 1000L
    }
}
