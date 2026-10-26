package app.orchard.tv.live.parser

import app.orchard.tv.live.model.EpgProgram
import org.xml.sax.Attributes
import org.xml.sax.InputSource
import org.xml.sax.helpers.DefaultHandler
import java.io.InputStream
import java.text.ParsePosition
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import java.util.zip.GZIPInputStream
import javax.xml.parsers.SAXParserFactory

/** XMLTV parser with a bounded input and output so a broken feed cannot consume the TV's heap. */
class XmlTvParser(
    private val maxPrograms: Int = DEFAULT_MAX_PROGRAMS,
    private val maxInputBytes: Long = DEFAULT_MAX_INPUT_BYTES,
) {
    fun parse(input: InputStream): List<EpgProgram> = parseInternal(BoundedInputStream(input, maxInputBytes))

    fun parseGzip(input: InputStream): List<EpgProgram> =
        GZIPInputStream(BoundedInputStream(input, maxInputBytes)).use { parseInternal(BoundedInputStream(it, maxInputBytes)) }

    fun parseAuto(input: InputStream): List<EpgProgram> {
        val bytes = input.readBytesBounded(maxInputBytes)
        return if (bytes.size >= 2 && bytes[0].toInt() and 0xff == GZIP_MAGIC_0 && bytes[1].toInt() and 0xff == GZIP_MAGIC_1) {
            parseGzip(bytes.inputStream())
        } else {
            parse(bytes.inputStream())
        }
    }

    private fun parseInternal(input: InputStream): List<EpgProgram> {
        val result = ArrayList<EpgProgram>()
        val handler = Handler(result)
        val factory = SAXParserFactory.newInstance().apply {
            isNamespaceAware = false
            runCatching { setFeature("http://xml.org/sax/features/external-general-entities", false) }
            runCatching { setFeature("http://xml.org/sax/features/external-parameter-entities", false) }
            runCatching { setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false) }
        }
        factory.newSAXParser().xmlReader.apply {
            contentHandler = handler
            entityResolver = org.xml.sax.EntityResolver { _, _ -> InputSource("about:blank") }
            parse(InputSource(input))
        }
        return result
    }

    private inner class Handler(private val result: MutableList<EpgProgram>) : DefaultHandler() {
        private var current: MutableProgram? = null
        private var currentTag: String? = null
        private var currentText = StringBuilder()

        override fun startElement(uri: String?, localName: String?, qName: String, attributes: Attributes) {
            val name = qName.ifBlank { localName.orEmpty() }.lowercase()
            when (name) {
                "programme" -> {
                    if (current != null) finish()
                    current = MutableProgram(
                        channelId = attributes.getValueOrNull("channel") ?: return,
                        startMillis = parseTimestamp(attributes.getValueOrNull("start")) ?: return,
                        endMillis = parseTimestamp(attributes.getValueOrNull("stop")),
                    )
                }
                "title", "sub-title", "subtitle", "desc", "description" -> if (current != null) {
                    currentTag = name
                    currentText = StringBuilder()
                }
            }
        }

        override fun characters(ch: CharArray, start: Int, length: Int) {
            if (currentTag != null) currentText.append(ch, start, length)
        }

        override fun endElement(uri: String?, localName: String?, qName: String) {
            val name = qName.ifBlank { localName.orEmpty() }.lowercase()
            if (name == currentTag) {
                val text = currentText.toString().trim().takeIf { it.isNotEmpty() }
                current?.let {
                    when (name) {
                        "title" -> if (it.title == null) it.title = text
                        "sub-title", "subtitle" -> if (it.subtitle == null) it.subtitle = text
                        "desc", "description" -> if (it.description == null) it.description = text
                    }
                }
                currentTag = null
                currentText = StringBuilder()
            }
            if (name == "programme") finish()
        }

        private fun finish() {
            val value = current ?: return
            if (result.size < maxPrograms && !value.title.isNullOrBlank()) {
                result += EpgProgram(value.channelId, value.startMillis, value.endMillis, value.title!!, value.subtitle, value.description)
            }
            current = null
            currentTag = null
        }
    }

    private data class MutableProgram(
        val channelId: String,
        val startMillis: Long,
        val endMillis: Long?,
        var title: String? = null,
        var subtitle: String? = null,
        var description: String? = null,
    )

    private fun parseTimestamp(value: String?): Long? {
        val raw = value?.trim().orEmpty()
        if (raw.length < 14) return null
        val digits = raw.take(14)
        if (!digits.all(Char::isDigit)) return null
        val zoneToken = raw.substring(14).trim().split(Regex("\\s+"), limit = 2).firstOrNull().orEmpty()
        val zone = when {
            zoneToken.equals("Z", ignoreCase = true) -> "UTC"
            Regex("^[+-]\\d{4}$").matches(zoneToken) -> {
                val sign = zoneToken[0]
                "GMT$sign${zoneToken.substring(1, 3)}:${zoneToken.substring(3)}"
            }
            Regex("^[+-]\\d{2}:\\d{2}$").matches(zoneToken) -> "GMT$zoneToken"
            else -> "UTC"
        }
        val format = SimpleDateFormat("yyyyMMddHHmmss", Locale.US).apply {
            isLenient = false
            timeZone = TimeZone.getTimeZone(zone)
        }
        return format.parse(digits, ParsePosition(0))?.time
    }

    private fun Attributes.getValueOrNull(name: String): String? = getValue(name)?.takeIf { it.isNotBlank() }

    private class BoundedInputStream(private val source: InputStream, private val limit: Long) : java.io.FilterInputStream(source) {
        private var count = 0L
        override fun read(): Int {
            val value = super.read()
            if (value >= 0 && ++count > limit) throw IllegalArgumentException("XMLTV feed is too large")
            return value
        }

        override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
            val value = super.read(buffer, offset, length)
            if (value > 0) {
                count += value
                if (count > limit) throw IllegalArgumentException("XMLTV feed is too large")
            }
            return value
        }
    }

    private fun InputStream.readBytesBounded(limit: Long): ByteArray {
        val out = java.io.ByteArrayOutputStream()
        val buffer = ByteArray(8192)
        var count = 0L
        while (true) {
            val read = read(buffer)
            if (read < 0) break
            count += read
            if (count > limit) throw IllegalArgumentException("XMLTV feed is too large")
            out.write(buffer, 0, read)
        }
        return out.toByteArray()
    }

    private companion object {
        const val DEFAULT_MAX_PROGRAMS = 200_000
        const val DEFAULT_MAX_INPUT_BYTES = 64L * 1024 * 1024
        const val GZIP_MAGIC_0 = 0x1f
        const val GZIP_MAGIC_1 = 0x8b
    }
}
