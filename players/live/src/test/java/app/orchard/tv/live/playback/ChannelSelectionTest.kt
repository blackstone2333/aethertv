package app.orchard.tv.live.playback
import org.junit.Assert.assertEquals
import org.junit.Test
class ChannelSelectionTest {
    @Test fun knownRequestWinsBeforeOpeningDefaultChannel() {
        assertEquals(2, ChannelSelection.index(listOf("a", "b", "c"), "b", "c"))
    }
    @Test fun unknownRequestFallsBackToLastAndThenFirst() {
        assertEquals(1, ChannelSelection.index(listOf("a", "b"), "b", "https://not-a-channel"))
        assertEquals(0, ChannelSelection.index(listOf("a", "b"), "missing", "unknown"))
    }
    @Test fun noRequestResumesLastAndEmptyCatalogIsSafe() {
        assertEquals(1, ChannelSelection.index(listOf("a", "b"), "b", null))
        assertEquals(0, ChannelSelection.index(emptyList(), "b", "a"))
    }
}
