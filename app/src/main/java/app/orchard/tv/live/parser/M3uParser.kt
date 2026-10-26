package app.orchard.tv.live.parser

import app.orchard.tv.live.model.Channel
import app.orchard.tv.live.model.M3uPlaylist
import java.io.InputStream
import java.nio.charset.Charset

/** Parses the IPTV M3U dialect used by iptv-api and common playlist exporters. */
class M3uParser(
    private val maxChannels: Int = DEFAULT_MAX_CHANNELS,
    private val maxLineLength: Int = DEFAULT_MAX_LINE_LENGTH,
) {
    fun parse(input: InputStream): M3uPlaylist = parse(decode(input.readBounded(MAX_PLAYLIST_BYTES)))

    fun parse(text: String): M3uPlaylist {
        val channels = ArrayList<Channel>()
        var playlistEpg: String? = null
        var pending: Pending? = null

        text.removePrefix("\uFEFF").lineSequence().forEach { raw ->
            if (channels.size >= maxChannels) return@forEach
            val line = raw.trim()
            if (line.isEmpty()) return@forEach
            require(line.length <= maxLineLength) { "M3U line is too long" }

            when {
                line.startsWith("#EXTM3U", ignoreCase = true) -> {
                    val attrs = attributes(line.substring(7))
                    playlistEpg = attrs.firstValue("url-tvg", "x-tvg-url", "tvg-url")
                }
                line.startsWith("#EXTINF:", ignoreCase = true) -> {
                    pending = parseExtInf(line)
                }
                line.startsWith("#") -> Unit
                pending != null -> {
                    val url = line.takeIf(::isStreamUrl)
                    if (url != null) {
                        channels += pending!!.toChannel(url)
                    }
                    pending = null
                }
            }
        }

        return M3uPlaylist(mergeDuplicates(channels), playlistEpg)
    }

    private fun parseExtInf(line: String): Pending {
        val body = line.substringAfter(':', "")
        val comma = body.indexOf(',')
        val metadata = if (comma >= 0) body.substring(0, comma) else body
        val displayName = if (comma >= 0) body.substring(comma + 1).trim() else ""
        val attrs = attributes(metadata)
        val name = displayName.takeIf { it.isNotBlank() }
            ?: attrs.firstValue("tvg-name")?.takeIf { it.isNotBlank() }
            ?: "Unknown channel"
        return Pending(
            name = name,
            tvgId = attrs.firstValue("tvg-id"),
            tvgName = attrs.firstValue("tvg-name"),
            logoUrl = attrs.firstValue("tvg-logo"),
            groupTitle = attrs.firstValue("group-title"),
        )
    }

    private fun mergeDuplicates(channels: List<Channel>): List<Channel> {
        val merged = LinkedHashMap<String, Channel>()
        channels.forEach { channel ->
            val key = channel.tvgId?.takeIf { it.isNotBlank() }
                ?: "${channel.groupTitle.orEmpty()}\u0000${channel.name}".lowercase()
            val previous = merged[key]
            merged[key] = if (previous == null) channel else previous.copy(
                streamUrls = (previous.streamUrls + channel.streamUrls).distinct(),
                logoUrl = previous.logoUrl ?: channel.logoUrl,
                tvgName = previous.tvgName ?: channel.tvgName,
            )
        }
        return merged.values.toList()
    }

    private fun isStreamUrl(value: String): Boolean {
        val lower = value.lowercase()
        return lower.startsWith("http://") || lower.startsWith("https://") ||
            lower.startsWith("rtsp://") || lower.startsWith("rtp://") || lower.startsWith("udp://")
    }

    private data class Pending(
        val name: String,
        val tvgId: String?,
        val tvgName: String?,
        val logoUrl: String?,
        val groupTitle: String?,
    ) {
        fun toChannel(url: String) = Channel(
            id = tvgId?.takeIf { it.isNotBlank() } ?: stableId(name, groupTitle),
            name = name,
            tvgId = tvgId,
            tvgName = tvgName,
            logoUrl = logoUrl,
            groupTitle = groupTitle,
            streamUrls = listOf(url),
        )
    }

    private fun Map<String, String>.firstValue(vararg keys: String): String? =
        keys.asSequence().mapNotNull { this[it.lowercase()] }.firstOrNull { it.isNotBlank() }

    private fun attributes(text: String): Map<String, String> {
        val result = LinkedHashMap<String, String>()
        var index = 0
        while (index < text.length) {
            while (index < text.length && text[index].isWhitespace()) index++
            val keyStart = index
            while (index < text.length && (text[index].isLetterOrDigit() || text[index] == '-' || text[index] == '_')) index++
            if (keyStart == index) {
                index++
                continue
            }
            val key = text.substring(keyStart, index).lowercase()
            while (index < text.length && text[index].isWhitespace()) index++
            if (index >= text.length || text[index] != '=') continue
            index++
            while (index < text.length && text[index].isWhitespace()) index++
            if (index >= text.length) break
            val value = if (text[index] == '"' || text[index] == '\'') {
                val quote = text[index++]
                val start = index
                while (index < text.length && text[index] != quote) index++
                text.substring(start, index).also { if (index < text.length) index++ }
            } else {
                val start = index
                while (index < text.length && !text[index].isWhitespace()) index++
                text.substring(start, index)
            }
            result.putIfAbsent(key, value)
        }
        return result
    }

    private fun decode(bytes: ByteArray): String {
        val utf8 = bytes.toString(Charsets.UTF_8)
        return if ('\uFFFD' in utf8) {
            runCatching { bytes.toString(Charset.forName("GB18030")) }.getOrDefault(utf8)
        } else utf8
    }

    private fun InputStream.readBounded(maxBytes: Long): ByteArray {
        val output = java.io.ByteArrayOutputStream()
        val buffer = ByteArray(8192)
        var total = 0L
        while (true) {
            val count = read(buffer)
            if (count < 0) break
            total += count
            require(total <= maxBytes) { "M3U playlist is too large" }
            output.write(buffer, 0, count)
        }
        return output.toByteArray()
    }

    private companion object {
        const val DEFAULT_MAX_CHANNELS = 20_000
        const val DEFAULT_MAX_LINE_LENGTH = 16 * 1024
        const val MAX_PLAYLIST_BYTES = 16L * 1024 * 1024

        fun stableId(name: String, group: String?): String =
            "${group.orEmpty()}\u0000$name".lowercase().replace(Regex("\\s+"), " ").trim()
    }
}
