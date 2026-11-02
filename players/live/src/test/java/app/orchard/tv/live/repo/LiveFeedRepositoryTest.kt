package app.orchard.tv.live.repo

import app.orchard.tv.live.model.LiveSourceConfig
import java.nio.file.Files
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LiveFeedRepositoryTest {
    @Test
    fun returnsFreshNetworkDataThenStaleCacheWhenOffline() {
        val directory = Files.createTempDirectory("aether-live-feed").toFile()
        try {
            var online = true
            val fetcher = LiveHttpFetcher { _, _, _ ->
                if (!online) error("offline")
                "#EXTM3U\n#EXTINF:-1 tvg-id=one,One\nhttps://stream/one".toByteArray()
            }
            val repository = LiveFeedRepository(
                cache = LiveCacheStore(directory, ttlMillis = 0),
                fetcher = fetcher,
            )
            val config = LiveSourceConfig("lan", m3uUrl = "http://nas/m3u")
            val first = repository.loadPlaylist(config)
            assertTrue(first is FeedLoad.Success)
            assertEquals(false, (first as FeedLoad.Success).value.stale)

            online = false
            val fallback = repository.loadPlaylist(config)
            assertTrue(fallback is FeedLoad.Success)
            val cached = fallback as FeedLoad.Success
            assertEquals(true, cached.value.stale)
            assertEquals("one", cached.value.playlist.channels.single().id)
        } finally {
            directory.deleteRecursively()
        }
    }
}
