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
@Config(sdk = [23, 28])
class LiveSourceStoreTest {
    @Test
    fun migrationAddsMissingIdsWithoutReplacingAndIsIdempotent() {
        LiveSources.init(RuntimeEnvironment.getApplication())
        LiveSources.remove("migration-old"); LiveSources.remove("migration-new")
        LiveSources.upsert(LiveSourceConfig("migration-old", m3uUrl = "https://kept.test/list"))
        val raw = """[{"id":"migration-old","m3uUrl":"https://replace.test/list"},{"id":"migration-new","m3uUrl":"https://new.test/list","headers":{"Authorization":"test"}}]"""
        assertEquals(1, LiveSources.importMissing(raw))
        assertEquals(0, LiveSources.importMissing(raw))
        assertEquals("https://kept.test/list", LiveSources.all().single { it.id == "migration-old" }.m3uUrl)
        assertEquals("test", LiveSources.all().single { it.id == "migration-new" }.headers["Authorization"])
        LiveSources.remove("migration-old"); LiveSources.remove("migration-new")
    }

    @Test
    fun invalidMigrationDoesNotPartiallyWrite() {
        LiveSources.init(RuntimeEnvironment.getApplication())
        LiveSources.remove("migration-atomic")
        val result = runCatching { LiveSources.importMissing("""[{"id":"migration-atomic","m3uUrl":"https://ok.test/list"},{"id":"bad","m3uUrl":"file:///private"}]""") }
        assertTrue(result.isFailure)
        assertTrue(LiveSources.all().none { it.id == "migration-atomic" })
    }

    @Test
    fun savesOnlyHttpSourcesAndPersistsTheirPriority() {
        LiveSources.init(RuntimeEnvironment.getApplication())
        LiveSources.remove("unit-lan")
        LiveSources.upsert(LiveSourceConfig("unit-lan", m3uUrl = "http://192.168.1.2:5181/m3u", priority = -2, label = "Family TV"))

        val source = LiveSources.all().single { it.id == "unit-lan" }
        assertEquals("Family TV", source.label)
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
