package app.orchard.tv.live.parser

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class M3uParserTest {
    @Test
    fun parsesHeaderAttributesAndMergesDuplicateChannels() {
        val playlist = M3uParser().parse(
            """#EXTM3U url-tvg="https://guide.example/epg.xml.gz"
               #EXTINF:-1 tvg-id="cctv1" tvg-name="CCTV 1" tvg-logo="https://img/c1.png" group-title="央视频道",CCTV-1
               https://stream.example/cctv1-a.m3u8
               #EXTINF:-1 tvg-id="cctv1" group-title="央视频道",备用
               https://stream.example/cctv1-b.m3u8
               #EXTINF:-1 group-title="卫视频道",湖南卫视
               rtp://239.0.0.1:1234
               #EXTINF:-1,坏地址
               file:///tmp/not-a-stream
            """.trimIndent(),
        )

        assertEquals("https://guide.example/epg.xml.gz", playlist.epgUrl)
        assertEquals(2, playlist.channels.size)
        val cctv = playlist.channels.first()
        assertEquals("cctv1", cctv.id)
        assertEquals("CCTV-1", cctv.name)
        assertEquals("CCTV 1", cctv.tvgName)
        assertEquals("央视频道", cctv.groupTitle)
        assertEquals(2, cctv.streamUrls.size)
        assertEquals("rtp://239.0.0.1:1234", playlist.channels[1].streamUrls.single())
        assertNull(playlist.channels.firstOrNull { it.name == "坏地址" })
    }

    @Test
    fun usesDisplayNameAndAllowsUnquotedAttributes() {
        val channel = M3uParser().parse(
            "#EXTINF:-1 tvg-id=foo group-title=News,Display Name\nhttps://example.test/live",
        ).channels.single()

        assertEquals("Display Name", channel.name)
        assertEquals("foo", channel.tvgId)
        assertEquals("News", channel.groupTitle)
        assertTrue(channel.id.isNotBlank())
        assertNotNull(channel.streamUrls.single())
    }
}
