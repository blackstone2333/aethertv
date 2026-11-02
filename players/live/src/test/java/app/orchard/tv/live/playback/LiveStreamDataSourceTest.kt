package app.orchard.tv.live.playback

import android.net.Uri
import androidx.media3.datasource.DataSpec
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.EOFException
import java.net.ServerSocket
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class LiveStreamDataSourceTest {
    private fun server(reply: String, check: (String) -> Unit = {}, block: (String) -> Unit) {
        ServerSocket(0).use { server ->
            server.soTimeout = 5000
            val pool = Executors.newSingleThreadExecutor()
            val done = pool.submit {
                server.accept().use { socket ->
                    socket.soTimeout = 5000
                    val reader = socket.getInputStream().bufferedReader()
                    val request = buildString {
                        while (true) { val line = reader.readLine() ?: break; if (line.isEmpty()) break; appendLine(line) }
                    }
                    check(request)
                    socket.getOutputStream().write(reply.toByteArray())
                }
            }
            try { block("http://127.0.0.1:${server.localPort}/live"); done.get(5, TimeUnit.SECONDS) }
            finally { pool.shutdownNow() }
        }
    }
    private fun read(source: LiveStreamDataSource): String {
        val buffer = ByteArray(2)
        val bytes = java.io.ByteArrayOutputStream()
        while (true) { val count = source.read(buffer, 0, buffer.size); if (count < 0) break; bytes.write(buffer, 0, count) }
        return bytes.toString("UTF-8")
    }

    @Test fun streamsChunkedBodiesWithoutBufferingTheWholeFeed() {
        server("HTTP/1.1 200 OK\r\nTransfer-Encoding: chunked\r\n\r\n3\r\nabc\r\n2;ext=yes\r\nde\r\n0\r\n\r\n") { url ->
            val source = LiveStreamDataSource(url, emptyMap(), 2000)
            try { assertEquals(-1L, source.open(DataSpec(Uri.parse(url)))); assertEquals("abcde", read(source)) }
            finally { source.close() }
        }
    }

    @Test fun rangedHlsSegmentsHonorPositionAndLength() {
        server("HTTP/1.1 206 Partial Content\r\nContent-Length: 3\r\nContent-Range: bytes 2-4/5\r\n\r\ncde", {
            assertTrue(it.contains("Range: bytes=2-4"))
        }) { url ->
            val source = LiveStreamDataSource(url, emptyMap(), 2000)
            try { assertEquals(3L, source.open(DataSpec.Builder().setUri(url).setPosition(2).setLength(3).build())); assertEquals("cde", read(source)) }
            finally { source.close() }
        }
    }

    @Test fun redirectsNeverForwardCredentialsToAnotherOrigin() {
        server("HTTP/1.1 200 OK\r\nContent-Length: 2\r\n\r\nok", { assertFalse(it.contains("Authorization", true)) }) { destination ->
            server("HTTP/1.1 302 Found\r\nLocation: $destination\r\nContent-Length: 0\r\n\r\n", {
                assertTrue(it.contains("Authorization: Bearer test"))
            }) { url ->
                val source = LiveStreamDataSource(url, mapOf("Authorization" to "Bearer test"), 2000)
                try { source.open(DataSpec(Uri.parse(url))); assertEquals("ok", read(source)) }
                finally { source.close() }
            }
        }
    }

    @Test fun aTruncatedBodyFailsInsteadOfReportingSuccessfulEndOfStream() {
        server("HTTP/1.1 200 OK\r\nContent-Length: 10\r\n\r\nabc") { url ->
            val source = LiveStreamDataSource(url, emptyMap(), 2000)
            try { source.open(DataSpec(Uri.parse(url))); assertThrows(EOFException::class.java) { read(source) } }
            finally { source.close() }
        }
    }
}
