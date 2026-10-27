package app.orchard.tv.live.playback

import app.orchard.tv.live.model.Channel
import app.orchard.tv.live.model.LiveSourceConfig
import org.junit.Assert.*
import org.junit.Test

class LiveStationTest {
    @Test fun combinesMatchingChannelsInPriorityOrderWithTheirOwnHeaders() {
        val lan = LiveSourceConfig("lan", m3uUrl = "http://lan/list", priority = 0, headers = mapOf("Authorization" to "local"))
        val backup = LiveSourceConfig("backup", m3uUrl = "https://backup/list", priority = 10)
        val channel = Channel("one", "News", tvgId = "news", streamUrls = listOf("http://lan/one", "http://lan/one"))
        val stations = mergeStations(listOf(backup to listOf(channel.copy(streamUrls = listOf("https://backup/one"))), lan to listOf(channel)))
        assertEquals(1, stations.size)
        assertEquals(listOf("lan", "backup"), stations.single().attempts.map { it.source.id })
        assertEquals("local", stations.single().attempts.first().source.headers["Authorization"])
    }

    @Test fun excludesDisabledSourcesAndUnsupportedOrCredentialEmbeddedUrls() {
        val source = LiveSourceConfig("one", m3uUrl = "http://source/list")
        val channels = listOf(Channel("one", "one", streamUrls = listOf("file:///private", "rtsp://host/live", "http://user:secret@host/live")))
        assertTrue(mergeStations(listOf(source to channels)).isEmpty())
        assertTrue(mergeStations(listOf(source.copy(enabled = false) to listOf(channels.single().copy(streamUrls = listOf("http://source/live"))))).isEmpty())
    }
}
