package app.orchard.tv.live.repo

import app.orchard.tv.live.model.Channel
import app.orchard.tv.live.model.LiveSourceConfig
import app.orchard.tv.live.model.LiveSourceKind
import org.junit.Assert.assertEquals
import org.junit.Test

class FailoverPlannerTest {
    private val lan = LiveSourceConfig("lan", LiveSourceKind.Lan, "http://nas/m3u", priority = 0)
    private val public = LiveSourceConfig("public", LiveSourceKind.Public, "https://cdn/m3u", priority = 1)
    private val ipv6 = LiveSourceConfig("ipv6", LiveSourceKind.Ipv6, "https://[::1]/m3u", priority = 2)

    @Test
    fun ordersEnabledSourcesByPriorityAndSkipsFailedOnes() {
        assertEquals(listOf("lan", "public", "ipv6"), FailoverPlanner.orderSources(listOf(ipv6, public, lan)).map { it.id })
        assertEquals(listOf("public", "ipv6"), FailoverPlanner.orderSources(listOf(lan, public, ipv6), setOf("lan")).map { it.id })
    }

    @Test
    fun plansUniqueUrlsForMergedPlaylists() {
        val channel = Channel("cctv1", "CCTV 1", tvgId = "cctv1", sourceId = "lan", streamUrls = listOf("http://nas/cctv1"))
        val attempts = FailoverPlanner.plan(
            channel,
            listOf(public, lan),
            mapOf(
                "lan" to listOf("http://nas/cctv1", "http://nas/cctv1"),
                "public" to listOf("https://cdn/cctv1"),
            ),
        )

        assertEquals(listOf("lan", "public"), attempts.map { it.source.id })
        assertEquals(listOf("http://nas/cctv1", "https://cdn/cctv1"), attempts.map { it.url }.distinct())
    }
}
