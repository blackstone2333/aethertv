package app.orchard.tv.live.repo

import app.orchard.tv.live.model.LiveSourceConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class LiveSourceStoreTest {
    @Test
    fun savesOnlyHttpSourcesAndPersistsTheirPriority() {
        LiveSources.init(RuntimeEnvironment.getApplication())
        LiveSources.remove("unit-lan")
        LiveSources.upsert(LiveSourceConfig("unit-lan", m3uUrl = "http://192.168.1.2:5181/m3u", priority = -2))

        val source = LiveSources.all().single { it.id == "unit-lan" }
        assertEquals(-2, source.priority)
        assertEquals("http://192.168.1.2:5181/m3u", source.m3uUrl)
        assertTrue(LiveSources.snapshot().getInt("sourceCount") >= 1)
        LiveSources.remove("unit-lan")
    }

    @Test
    fun rejectsNonHttpPlaylistAddresses() {
        LiveSources.init(RuntimeEnvironment.getApplication())
        var rejected = false
        try {
            LiveSources.upsert(LiveSourceConfig("unit-file", m3uUrl = "file:///tmp/list.m3u"))
        } catch (_: IllegalArgumentException) {
            rejected = true
        } finally {
            LiveSources.remove("unit-file")
        }
        assertTrue(rejected)
    }
}
