package app.orchard.tv.live.repo

import java.nio.file.Files
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LiveCacheStoreTest {
    @Test
    fun marksEntriesStaleAfterTtlButKeepsOfflineFallback() {
        val directory = Files.createTempDirectory("aether-live-cache").toFile()
        try {
            val cache = LiveCacheStore(directory, ttlMillis = 100, maxEntryBytes = 100, maxTotalBytes = 200)
            assertTrue(cache.write("m3u", "cached".toByteArray(), now = 1_000))
            assertTrue(cache.read("m3u", now = 1_050)!!.fresh)
            val stale = cache.read("m3u", now = 1_101)
            assertNotNull(stale)
            assertFalse(stale!!.fresh)
            assertEquals("cached", stale.bytes.toString(Charsets.UTF_8))
        } finally {
            directory.deleteRecursively()
        }
    }

    @Test
    fun prunesOldEntriesWhenTotalLimitIsReached() {
        val directory = Files.createTempDirectory("aether-live-cache").toFile()
        try {
            val cache = LiveCacheStore(directory, maxEntryBytes = 8, maxTotalBytes = 10)
            assertTrue(cache.write("old", ByteArray(8) { 1 }, now = 1_000))
            assertTrue(cache.write("new", ByteArray(8) { 2 }, now = 2_000))
            assertNull(cache.read("old", now = 2_001))
            assertNotNull(cache.read("new", now = 2_001))
        } finally {
            directory.deleteRecursively()
        }
    }
}
