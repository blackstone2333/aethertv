package app.orchard.tv.live.repo

import java.io.ByteArrayOutputStream
import java.io.EOFException
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.InetSocketAddress
import java.net.Socket
import java.net.URI
import java.util.Locale

fun interface LiveHttpFetcher {
    fun fetch(url: String, timeoutMillis: Int, headers: Map<String, String>): ByteArray
}

/**
 * Only explicitly configured IPTV feeds use plain HTTP. A socket keeps this exception local to
 * this repository instead of allowing cleartext for every library/domain in the application.
 * HTTPS keeps the platform's certificate and hostname checks. No cookies or global SSL changes.
 */
class UrlConnectionFetcher(private val maxBytes: Long = 64L * 1024 * 1024) : LiveHttpFetcher {
    override fun fetch(url: String, timeoutMillis: Int, headers: Map<String, String>): ByteArray {
        var uri = checkedUri(URI(url))
        var requestHeaders = headers.filterKeys { it.lowercase(Locale.ROOT) !in RESERVED_HEADERS }
        requestHeaders.forEach { (key, value) ->
            require(HEADER_NAME.matches(key) && value.all { it == '\t' || it.code in 32..126 }) { "invalid feed header" }
        }
        repeat(6) { hop ->
            val response = if (uri.scheme.equals("http", true)) http(uri, timeoutMillis, requestHeaders)
                else https(uri, timeoutMillis, requestHeaders)
            if (response.code in REDIRECTS) {
                if (hop == 5) throw IOException("too many redirects")
                val next = checkedUri(uri.resolve(response.location ?: throw IOException("redirect without location")))
                if (uri.scheme.equals("https", true) && next.scheme.equals("http", true)) {
                    throw IOException("insecure redirect")
                }
                // A redirect to a different origin must never receive a source's credentials.
                if (origin(uri) != origin(next)) requestHeaders = emptyMap()
                uri = next
            } else {
                if (response.code !in 200..299) throw IOException("HTTP ${response.code}")
                return response.body
            }
        }
        throw IOException("too many redirects")
    }

    private fun https(uri: URI, timeout: Int, headers: Map<String, String>): Response {
        val connection = uri.toURL().openConnection() as HttpURLConnection
        return try {
            connection.connectTimeout = timeout.coerceAtLeast(1)
            connection.readTimeout = timeout.coerceAtLeast(1)
            connection.instanceFollowRedirects = false
            connection.useCaches = false
            connection.setRequestProperty("Accept-Encoding", "identity")
            headers.forEach { (key, value) -> connection.setRequestProperty(key, value) }
            val code = connection.responseCode
            if (code !in 200..299) Response(code, connection.getHeaderField("Location"))
            else {
                // contentLengthLong is unavailable on the oldest supported Android releases.
                val length = connection.getHeaderField("Content-Length")?.toLongOrNull()
                Response(code, body = connection.inputStream.use { readBody(it, length) })
            }
        } finally {
            connection.disconnect()
        }
    }

    private fun http(uri: URI, timeout: Int, headers: Map<String, String>): Response = Socket().use { socket ->
        socket.connect(InetSocketAddress(uri.host.removeSurrounding("[", "]"), port(uri)), timeout.coerceAtLeast(1))
        socket.soTimeout = timeout.coerceAtLeast(1)
        val target = (uri.rawPath.takeUnless { it.isNullOrEmpty() } ?: "/") + (uri.rawQuery?.let { "?$it" } ?: "")
        require(target.all { it.code in 33..126 }) { "invalid feed path" }
        val host = uri.host + if (uri.port != -1) ":${uri.port}" else ""
        val request = buildString {
            append("GET $target HTTP/1.1\r\nHost: $host\r\nConnection: close\r\nAccept-Encoding: identity\r\n")
            headers.forEach { (key, value) -> append("$key: $value\r\n") }
            append("\r\n")
        }
        socket.getOutputStream().write(request.toByteArray(Charsets.ISO_8859_1))
        val input = socket.getInputStream().buffered()
        val status = readLine(input)
        val code = Regex("HTTP/1\\.[01] ([0-9]{3})(?: .*)?").matchEntire(status)?.groupValues?.get(1)?.toInt()
            ?: throw IOException("invalid HTTP status")
        val responseHeaders = linkedMapOf<String, String>()
        var headerBytes = status.length
        while (true) {
            val line = readLine(input)
            headerBytes += line.length + 2
            if (headerBytes > 64 * 1024) throw IOException("headers too large")
            if (line.isEmpty()) break
            val name = line.substringBefore(':').lowercase(Locale.ROOT)
            if (':' !in line || !HEADER_NAME.matches(name)) throw IOException("invalid HTTP header")
            if (name in responseHeaders && name in setOf("content-length", "transfer-encoding", "location")) {
                throw IOException("duplicate HTTP framing header")
            }
            responseHeaders[name] = line.substringAfter(':').trim()
        }
        if (code !in 200..299) return@use Response(code, responseHeaders["location"])
        val transfer = responseHeaders["transfer-encoding"]?.lowercase(Locale.ROOT)
        val length = responseHeaders["content-length"]?.let { it.toLongOrNull() ?: throw IOException("invalid length") }
        if (transfer != null && transfer != "chunked") throw IOException("unsupported transfer encoding")
        if (transfer != null && length != null) throw IOException("ambiguous body length")
        Response(code, body = if (transfer == "chunked") readChunks(input) else readBody(input, length))
    }

    private fun readChunks(input: InputStream): ByteArray {
        val out = ByteArrayOutputStream()
        while (true) {
            val length = readLine(input).substringBefore(';').trim().toLongOrNull(16)
                ?: throw IOException("invalid chunk length")
            if (length < 0 || length > maxBytes - out.size()) throw IOException("response too large")
            if (length == 0L) return out.toByteArray()
            copy(input, out, length)
            if (readLine(input).isNotEmpty()) throw IOException("invalid chunk ending")
        }
    }

    private fun readBody(input: InputStream, length: Long?): ByteArray {
        if (length != null && (length < 0 || length > maxBytes)) throw IOException("response too large")
        val out = ByteArrayOutputStream()
        if (length != null) copy(input, out, length)
        else {
            val buffer = ByteArray(8192)
            while (true) {
                val count = input.read(buffer)
                if (count < 0) break
                if (out.size().toLong() + count > maxBytes) throw IOException("response too large")
                out.write(buffer, 0, count)
            }
        }
        return out.toByteArray()
    }

    private fun copy(input: InputStream, out: ByteArrayOutputStream, length: Long) {
        var remaining = length
        val buffer = ByteArray(8192)
        while (remaining > 0) {
            val count = input.read(buffer, 0, minOf(buffer.size.toLong(), remaining).toInt())
            if (count < 0) throw EOFException("truncated response")
            out.write(buffer, 0, count)
            remaining -= count
        }
    }

    private fun readLine(input: InputStream): String {
        val out = ByteArrayOutputStream()
        while (out.size() <= 8192) {
            val value = input.read()
            if (value < 0) throw EOFException("truncated HTTP line")
            if (value == 10) {
                val bytes = out.toByteArray()
                if (bytes.lastOrNull() != 13.toByte()) throw IOException("invalid HTTP line ending")
                return String(bytes, 0, bytes.size - 1, Charsets.ISO_8859_1)
            }
            out.write(value)
        }
        throw IOException("HTTP line too large")
    }

    private fun checkedUri(uri: URI): URI {
        require((uri.scheme.equals("http", true) || uri.scheme.equals("https", true)) && !uri.host.isNullOrBlank() && uri.rawUserInfo == null) {
            "feed URL must use http(s) without embedded credentials"
        }
        require(uri.port == -1 || uri.port in 1..65535) { "invalid feed port" }
        return uri
    }

    private fun port(uri: URI) = if (uri.port != -1) uri.port else if (uri.scheme.equals("https", true)) 443 else 80
    private fun origin(uri: URI) = Triple(uri.scheme.lowercase(Locale.ROOT), uri.host.lowercase(Locale.ROOT), port(uri))
    private data class Response(val code: Int, val location: String? = null, val body: ByteArray = byteArrayOf())

    companion object {
        private val REDIRECTS = setOf(301, 302, 303, 307, 308)
        private val HEADER_NAME = Regex("[!#$%&'*+.^_`|~0-9A-Za-z-]+")
        private val RESERVED_HEADERS = setOf("host", "connection", "content-length", "transfer-encoding", "accept-encoding")
    }
}
