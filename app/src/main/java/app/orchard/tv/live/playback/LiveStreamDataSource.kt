package app.orchard.tv.live.playback

import android.net.Uri
import androidx.media3.common.C
import androidx.media3.datasource.BaseDataSource
import androidx.media3.datasource.DataSpec
import java.io.EOFException
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.InetSocketAddress
import java.net.Socket
import java.net.URI
import java.util.Locale

/** Streaming-only HTTP exception for owner-configured IPTV; never changes global network policy. */
class LiveStreamDataSource(private val credentialOrigin: String, private val headers: Map<String, String>,
                           private val timeoutMillis: Int) : BaseDataSource(true) {
    private var input: InputStream? = null
    private var disconnect: (() -> Unit)? = null
    private var current: Uri? = null
    private var remaining = C.LENGTH_UNSET.toLong()
    private var opened = false
    private var responseHeaders: Map<String, List<String>> = emptyMap()

    override fun getUri(): Uri? = current
    override fun getResponseHeaders(): Map<String, List<String>> = responseHeaders

    override fun open(dataSpec: DataSpec): Long {
        transferInitializing(dataSpec)
        var uri = checked(URI(dataSpec.uri.toString()))
        try {
            repeat(6) { hop ->
                val request = linkedMapOf("User-Agent" to "AetherTV", "Accept-Encoding" to "identity")
                if (origin(uri) == origin(URI(credentialOrigin))) headers.forEach { (key, value) ->
                    if (key.lowercase(Locale.ROOT) !in RESERVED) {
                        require(HEADER.matches(key) && value.all { it == '\t' || it.code in 32..126 })
                        request[key] = value
                    }
                }
                if (dataSpec.position > 0 || dataSpec.length != C.LENGTH_UNSET.toLong()) {
                    val end = if (dataSpec.length == C.LENGTH_UNSET.toLong()) "" else (dataSpec.position + dataSpec.length - 1).toString()
                    request["Range"] = "bytes=${dataSpec.position}-$end"
                }
                val response = connect(uri, request)
                if (response.code in REDIRECTS) {
                    if (hop == 5) throw IOException("redirect limit")
                    val next = checked(uri.resolve(response.headers["location"] ?: throw IOException("missing redirect")))
                    if (uri.scheme.equals("https", true) && next.scheme.equals("http", true)) throw IOException("insecure redirect")
                    disconnect?.invoke(); disconnect = null
                    uri = next
                } else {
                    if (response.code !in 200..299) throw IOException("stream HTTP ${response.code}")
                    val stream = response.body ?: throw IOException("empty stream")
                    input = stream
                    val encoding = response.headers["content-encoding"]
                    if (encoding != null && !encoding.equals("identity", true)) throw IOException("encoded stream")
                    if (response.code == 206) {
                        val start = response.headers["content-range"]?.substringAfter("bytes ")?.substringBefore('-')?.toLongOrNull()
                        if (start != dataSpec.position) throw IOException("invalid stream range")
                    } else if (dataSpec.position > 0) {
                        var skip = dataSpec.position
                        val buffer = ByteArray(8192)
                        while (skip > 0) {
                            val count = stream.read(buffer, 0, minOf(skip, buffer.size.toLong()).toInt())
                            if (count < 0) throw EOFException("stream range beyond end")
                            skip -= count
                        }
                    }
                    val available = response.headers["content-length"]?.toLongOrNull()?.let {
                        if (response.code == 206) it else (it - dataSpec.position).coerceAtLeast(0)
                    }
                    remaining = if (dataSpec.length != C.LENGTH_UNSET.toLong()) dataSpec.length else available ?: C.LENGTH_UNSET.toLong()
                    current = Uri.parse(uri.toString())
                    responseHeaders = response.headers.mapValues { listOf(it.value) }
                    opened = true
                    transferStarted(dataSpec)
                    return remaining
                }
            }
            throw IOException("redirect limit")
        } catch (error: Exception) { close(); throw error }
    }

    override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
        if (length == 0) return 0
        if (remaining == 0L) return C.RESULT_END_OF_INPUT
        val count = input?.read(buffer, offset, if (remaining < 0) length else minOf(length.toLong(), remaining).toInt()) ?: C.RESULT_END_OF_INPUT
        if (count < 0 && remaining > 0) throw EOFException("truncated stream")
        if (count > 0) { if (remaining >= 0) remaining -= count; bytesTransferred(count) }
        return count
    }

    override fun close() {
        runCatching { input?.close() }; input = null
        runCatching { disconnect?.invoke() }; disconnect = null
        current = null; responseHeaders = emptyMap()
        if (opened) { opened = false; transferEnded() }
    }

    private data class Response(val code: Int, val headers: Map<String, String>, val body: InputStream?)

    private fun connect(uri: URI, request: Map<String, String>): Response {
        if (uri.scheme.equals("https", true)) {
            val connection = uri.toURL().openConnection() as HttpURLConnection
            disconnect = { connection.disconnect() }
            connection.connectTimeout = timeoutMillis; connection.readTimeout = timeoutMillis
            connection.instanceFollowRedirects = false; connection.useCaches = false
            request.forEach { (key, value) -> connection.setRequestProperty(key, value) }
            val code = connection.responseCode
            val fields = connection.headerFields.filterKeys { it != null }.mapKeys { it.key.lowercase(Locale.ROOT) }.mapValues { it.value.joinToString(",") }
            return Response(code, fields, if (code in 200..299) connection.inputStream else null)
        }
        val socket = Socket()
        disconnect = { socket.close() }
        socket.connect(InetSocketAddress(uri.host.removeSurrounding("[", "]"), if (uri.port > 0) uri.port else 80), timeoutMillis)
        socket.soTimeout = timeoutMillis
        val path = (uri.rawPath.takeUnless { it.isNullOrEmpty() } ?: "/") + (uri.rawQuery?.let { "?$it" } ?: "")
        require(path.all { it.code in 33..126 })
        val host = uri.host + if (uri.port > 0) ":${uri.port}" else ""
        socket.getOutputStream().write(buildString {
            append("GET $path HTTP/1.1\r\nHost: $host\r\nConnection: close\r\n")
            request.forEach { (key, value) -> append("$key: $value\r\n") }; append("\r\n")
        }.toByteArray(Charsets.ISO_8859_1))
        val body = socket.getInputStream().buffered()
        val status = line(body)
        val code = Regex("HTTP/1\\.[01] ([0-9]{3})(?: .*)?").matchEntire(status)?.groupValues?.get(1)?.toInt() ?: throw IOException("invalid stream status")
        val fields = linkedMapOf<String, String>()
        var size = status.length
        while (true) {
            val header = line(body); size += header.length + 2
            if (size > 65536) throw IOException("stream headers too large")
            if (header.isEmpty()) break
            val name = header.substringBefore(':').lowercase(Locale.ROOT)
            if (':' !in header || !HEADER.matches(name)) throw IOException("invalid stream header")
            if (name in fields && name in setOf("content-length", "transfer-encoding", "location")) throw IOException("ambiguous stream headers")
            fields[name] = header.substringAfter(':').trim()
        }
        val transfer = fields["transfer-encoding"]?.lowercase(Locale.ROOT)
        if (transfer != null && (transfer != "chunked" || "content-length" in fields)) throw IOException("invalid stream framing")
        return Response(code, fields, if (transfer == "chunked") Chunks(body) else body)
    }

    private class Chunks(private val input: InputStream) : InputStream() {
        private var left = 0L
        private var first = true
        private var done = false
        override fun read(): Int = ByteArray(1).let { if (read(it, 0, 1) < 0) -1 else it[0].toInt() and 255 }
        override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
            if (length == 0) return 0
            if (done) return -1
            if (left == 0L) {
                if (!first && line(input).isNotEmpty()) throw IOException("invalid chunk boundary")
                first = false
                left = line(input).substringBefore(';').trim().toLongOrNull(16) ?: throw IOException("invalid chunk")
                if (left < 0) throw IOException("invalid chunk size")
                if (left == 0L) { done = true; return -1 }
            }
            val count = input.read(buffer, offset, minOf(left, length.toLong()).toInt())
            if (count < 0) throw EOFException("truncated chunk")
            left -= count
            return count
        }
        override fun close() = input.close()
    }

    companion object {
        private val HEADER = Regex("[!#$%&'*+.^_`|~0-9A-Za-z-]+")
        private val RESERVED = setOf("host", "connection", "content-length", "transfer-encoding", "accept-encoding", "range")
        private val REDIRECTS = setOf(301, 302, 303, 307, 308)
        private fun checked(uri: URI): URI {
            require(uri.scheme.lowercase(Locale.ROOT) in listOf("http", "https") && !uri.host.isNullOrEmpty() && uri.rawUserInfo == null && (uri.port == -1 || uri.port in 1..65535))
            return uri
        }
        private fun origin(uri: URI) = Triple(uri.scheme.lowercase(Locale.ROOT), uri.host.lowercase(Locale.ROOT), if (uri.port > 0) uri.port else if (uri.scheme.equals("https", true)) 443 else 80)
        private fun line(input: InputStream): String {
            val out = java.io.ByteArrayOutputStream()
            while (out.size() <= 8192) {
                val c = input.read()
                if (c < 0) throw EOFException("truncated stream header")
                if (c == 10) {
                    val bytes = out.toByteArray()
                    if (bytes.lastOrNull() != 13.toByte()) throw IOException("invalid stream line")
                    return String(bytes, 0, bytes.size - 1, Charsets.ISO_8859_1)
                }
                out.write(c)
            }
            throw IOException("stream line too long")
        }
    }
}
