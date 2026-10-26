package app.orchard.tv.live.parser

import java.io.ByteArrayOutputStream
import java.util.zip.GZIPOutputStream
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class XmlTvParserTest {
    private val xml = """<?xml version="1.0" encoding="UTF-8"?>
        <!DOCTYPE tv SYSTEM "https://invalid.example/tv.dtd">
        <tv>
          <programme channel="cctv1" start="20261026120000 +0800" stop="20261026130000 +0800">
            <title lang="zh">午间新闻</title>
            <sub-title>直播</sub-title>
            <desc>今日要闻</desc>
          </programme>
          <programme channel="cctv1" start="20261026130000 +0800" stop="20261026140000 +0800">
            <title>电影</title>
          </programme>
        </tv>""".trimIndent()

    @Test
    fun parsesProgramTimesAndText() {
        val programs = XmlTvParser().parse(xml.byteInputStream())

        assertEquals(2, programs.size)
        assertEquals("cctv1", programs[0].channelId)
        assertEquals("午间新闻", programs[0].title)
        assertEquals("直播", programs[0].subtitle)
        assertEquals("今日要闻", programs[0].description)
        assertEquals(3_600_000L, programs[0].endMillis!! - programs[0].startMillis)
        assertEquals(0L, programs[1].startMillis - programs[0].endMillis!!)
        assertNull(programs[1].subtitle)
    }

    @Test
    fun parsesGzipAutomatically() {
        val compressed = ByteArrayOutputStream().also { output ->
            GZIPOutputStream(output).use { it.write(xml.toByteArray()) }
        }.toByteArray()

        assertEquals(2, XmlTvParser().parseAuto(compressed.inputStream()).size)
    }
}
