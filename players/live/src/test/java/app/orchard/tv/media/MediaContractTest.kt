package app.orchard.tv.media
import org.junit.Assert.*
import org.junit.Test
class MediaContractTest {
    @Test fun validatesOpaqueChannelIds() {
        assertEquals("id:news", MediaContract.channelId("id:news"))
        assertNull(MediaContract.channelId(""))
        assertNull(MediaContract.channelId("x".repeat(257)))
        assertNull(MediaContract.channelId("news\nurl"))
    }
}
