package app.orchard.tv.weather

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class WeatherRefreshTest {
    @Test
    fun visibleHomeRefreshesHourlyWithoutRestartingTheActivity() = runTest {
        var reads = 0
        val refresh = WeatherRefresh(backgroundScope, { testScheduler.currentTime }) { reads++; true }
        refresh.refreshIfStale()
        refresh.whileShown(true)
        runCurrent()
        assertEquals(1, reads)
        repeat(2) { hour ->
            advanceTimeBy(HOUR + GRACE - 1)
            runCurrent()
            assertEquals(hour + 1, reads)
            advanceTimeBy(1)
            runCurrent()
            assertEquals(hour + 2, reads)
        }
    }

    @Test
    fun failedReadsBackOffFromThreeMinutesToAtMostAnHour() = runTest {
        var reads = 0
        val refresh = WeatherRefresh(backgroundScope, { testScheduler.currentTime }) { reads++; false }
        refresh.refreshIfStale()
        refresh.whileShown(true)
        runCurrent()
        listOf(3L, 6L, 12L, 24L, 48L, 60L, 60L).forEachIndexed { index, minutes ->
            advanceTimeBy(minutes * MINUTE + GRACE - 1)
            runCurrent()
            assertEquals(index + 1, reads)
            advanceTimeBy(1)
            runCurrent()
            assertEquals(index + 2, reads)
        }
    }

    @Test
    fun hidingHomeStopsTimerAndRepeatedResumeKeepsOnlyOneTimer() = runTest {
        var reads = 0
        val refresh = WeatherRefresh(backgroundScope, { testScheduler.currentTime }) { reads++; true }
        refresh.refreshIfStale()
        refresh.whileShown(true)
        runCurrent()
        refresh.whileShown(false)
        advanceTimeBy(3 * HOUR)
        runCurrent()
        assertEquals(1, reads)
        refresh.whileShown(true)
        refresh.whileShown(true)
        advanceTimeBy(GRACE)
        runCurrent()
        assertEquals(2, reads)
        advanceTimeBy(HOUR + GRACE)
        runCurrent()
        assertEquals(3, reads)
    }

    @Test
    fun lifecycleAndNetworkCallbacksDoNotRestartAnInflightRead() = runTest {
        var reads = 0
        val result = CompletableDeferred<Boolean>()
        val refresh = WeatherRefresh(backgroundScope, { testScheduler.currentTime }) { reads++; result.await() }
        refresh.refreshIfStale()
        refresh.whileShown(true)
        runCurrent()
        advanceTimeBy(2 * HOUR)
        refresh.refreshIfStale()
        refresh.refreshIfStale(networkCameUp = true)
        refresh.whileShown(false)
        refresh.whileShown(true)
        runCurrent()
        assertTrue(refresh.inFlight)
        assertFalse(result.isCancelled)
        assertEquals(1, reads)
        result.complete(true)
        runCurrent()
        assertFalse(refresh.inFlight)
    }

    @Test
    fun readFinishingAfterPauseDoesNotScheduleBackgroundWork() = runTest {
        var reads = 0
        val result = CompletableDeferred<Boolean>()
        val refresh = WeatherRefresh(backgroundScope, { testScheduler.currentTime }) { reads++; result.await() }
        refresh.whileShown(true)
        refresh.refreshIfStale()
        runCurrent()
        refresh.whileShown(false)
        result.complete(false)
        runCurrent()
        advanceTimeBy(3 * HOUR)
        runCurrent()
        assertEquals(1, reads)
    }

    @Test
    fun networkRecoveryRetriesEarlyAndSuccessResetsFailureBackoff() = runTest {
        var reads = 0
        val refresh = WeatherRefresh(backgroundScope, { testScheduler.currentTime }) { ++reads == 3 }
        refresh.whileShown(true)
        refresh.refreshIfStale()
        runCurrent()
        refresh.refreshIfStale(networkCameUp = true)
        runCurrent()
        assertEquals(2, reads)
        refresh.refreshIfStale(networkCameUp = true)
        runCurrent()
        assertEquals(3, reads)
        refresh.refreshIfStale(networkCameUp = true)
        runCurrent()
        assertEquals(3, reads)
        advanceTimeBy(HOUR + GRACE)
        runCurrent()
        assertEquals(4, reads)
        advanceTimeBy(3 * MINUTE + GRACE)
        runCurrent()
        assertEquals(5, reads)
    }

    @Test
    fun manualLocationRefreshReplacesLongTimerWithFailureRetry() = runTest {
        var reads = 0
        val refresh = WeatherRefresh(backgroundScope, { testScheduler.currentTime }) { ++reads == 1 }
        refresh.whileShown(true)
        refresh.refreshIfStale()
        runCurrent()
        advanceTimeBy(MINUTE)
        refresh.refresh()
        runCurrent()
        assertEquals(2, reads)
        advanceTimeBy(3 * MINUTE + GRACE)
        runCurrent()
        assertEquals(3, reads)
        refresh.refresh()
        runCurrent()
        advanceTimeBy(3 * MINUTE + GRACE)
        runCurrent()
        assertEquals(5, reads)
    }

    @Test
    fun manualLocationRefreshCancelsThePreviousRequest() = runTest {
        var reads = 0
        var cancelled = 0
        val result = CompletableDeferred<Boolean>()
        val refresh = WeatherRefresh(backgroundScope, { testScheduler.currentTime }) {
            reads++
            if (reads == 1) try { result.await() } finally { cancelled++ } else true
        }
        refresh.whileShown(true)
        refresh.refreshIfStale()
        runCurrent()
        refresh.refresh()
        runCurrent()
        assertEquals(1, cancelled)
        assertEquals(2, reads)
        assertFalse(refresh.inFlight)
        advanceTimeBy(HOUR + GRACE)
        runCurrent()
        assertEquals(3, reads)
    }

    private companion object {
        const val MINUTE = 60_000L
        const val HOUR = 60 * MINUTE
        const val GRACE = 1_000L
    }
}
