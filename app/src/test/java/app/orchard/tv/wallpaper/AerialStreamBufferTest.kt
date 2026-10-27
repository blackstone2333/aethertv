package app.orchard.tv.wallpaper

import androidx.media3.common.C
import androidx.media3.common.Timeline
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.LoadControl
import androidx.media3.exoplayer.analytics.PlayerId
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.exoplayer.source.TrackGroupArray
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@UnstableApi
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [23], manifest = Config.NONE)
class AerialStreamBufferTest {
    private fun params(bufferedUs: Long = 500_000, rebuffering: Boolean = false) = LoadControl.Parameters(
        PlayerId.UNSET, Timeline.EMPTY, MediaSource.MediaPeriodId("film"), 0, bufferedUs,
        1f, true, rebuffering, C.TIME_UNSET, C.TIME_UNSET)

    @Test fun liteStopsReadingAtItsByteBudgetEvenBeforeTheMinimumDuration() {
        for ((quality, mib) in listOf(AerialQuality.Hd to 8, AerialQuality.Uhd to 12, AerialQuality.DolbyVision to 16)) {
            val control = AerialStream.loadControl(quality, true)
            control.onPrepared(PlayerId.UNSET)
            control.onTracksSelected(params(), TrackGroupArray.EMPTY, emptyArray())
            val allocator = control.allocator
            val count = mib * 1024 * 1024 / allocator.individualAllocationLength
            val blocks = List(count - 1) { allocator.allocate() }
            assertTrue(control.shouldContinueLoading(params()))
            val last = allocator.allocate()
            assertFalse("$quality must honor $mib MiB", control.shouldContinueLoading(params()))
            blocks.forEach(allocator::release); allocator.release(last)
            control.onReleased(PlayerId.UNSET)
        }
    }

    @Test fun fullRetainsItsReadAheadAndLiteStillWaitsForRebufferingHeadroom() {
        val full = AerialStream.loadControl(AerialQuality.Hd, false)
        full.onPrepared(PlayerId.UNSET)
        full.onTracksSelected(params(), TrackGroupArray.EMPTY, emptyArray())
        val allocator = full.allocator
        val blocks = List(8 * 1024 * 1024 / allocator.individualAllocationLength) { allocator.allocate() }
        assertTrue(full.shouldContinueLoading(params()))
        blocks.forEach(allocator::release)
        full.onReleased(PlayerId.UNSET)
        val lite = AerialStream.loadControl(AerialQuality.Hd, true)
        lite.onPrepared(PlayerId.UNSET)
        assertTrue(lite.shouldStartPlayback(params(1_500_000)))
        assertFalse(lite.shouldStartPlayback(params(1_500_000, true)))
        lite.onReleased(PlayerId.UNSET)
    }
}
