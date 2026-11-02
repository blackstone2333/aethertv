package app.orchard.tv.live.repo

import java.io.IOException
import java.net.InetAddress
import java.net.ServerSocket
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class LiveHttpFetcherTest {
    @Test fun readsFixedLengthAndChunkedFeeds() {
        serve("HTTP/1.1 200 OK\r\nContent-Length: 4\r\n\r\nfeed") { url, _ ->
            assertEquals("feed", fetch(url))
        }
        serve("HTTP/1.1 200 OK\r\nTransfer-Encoding: chunked\r\n\r\n2\r\nfe\r\n2;ext=yes\r\ned\r\n0\r\n\r\n") { url, _ ->
            assertEquals("feed", fetch(url))
        }
    }

    @Test fun rejectsOversizedOrTruncatedBodies() {
        listOf(
            "HTTP/1.1 200 OK\r\nContent-Length: 5\r\n\r\nfeed",
            "HTTP/1.1 200 OK\r\nContent-Length: 3\r\n\r\nfe",
            "HTTP/1.1 200 OK\r\nTransfer-Encoding: chunked\r\n\r\n5\r\nabcde\r\n0\r\n\r\n",
            "HTTP/1.1 200 OK\r\n\r\nabcde",
            "HTTP/1.1 200 OK\r\nContent-Length: 4\r\nTransfer-Encoding: chunked\r\n\r\n",
        ).forEach { response ->
            serve(response) { url, _ -> assertThrows(IOException::class.java) { fetch(url) } }
        }
    }

    @Test fun stripsAllCustomHeadersWhenRedirectChangesOrigin() {
        serve("HTTP/1.1 200 OK\r\nContent-Length: 4\r\n\r\nfeed") { destination, received ->
            serve("HTTP/1.1 302 Found\r\nLocation: $destination\r\n\r\n") { url, _ ->
                assertEquals("feed", fetch(url, mapOf("Authorization" to "Bearer private", "X-Api-Key" to "private")))
            }
            assertFalse(received().contains("private"))
        }
    }

    @Test fun retainsHeadersOnSameOriginRedirectAndEncodesQuery() {
        val requests = mutableListOf<String>()
        ServerSocket(0, 5, InetAddress.getLoopbackAddress()).use { server ->
            val executor = Executors.newSingleThreadExecutor()
            val done = executor.submit {
                repeat(2) { index -> server.accept().use { socket ->
                    val reader = socket.getInputStream().bufferedReader()
                    requests += generateSequence { reader.readLine()?.takeIf { it.isNotEmpty() } }.joinToString("\n")
                    socket.getOutputStream().write((if (index == 0) "HTTP/1.1 302 Found\r\nLocation: /feed?q=one%20two\r\n\r\n"
                        else "HTTP/1.1 200 OK\r\nContent-Length: 4\r\n\r\nfeed").toByteArray())
                } }
            }
            try {
                assertEquals("feed", fetch("http://localhost:${server.localPort}/start", mapOf("Authorization" to "Bearer private")))
                done.get(5, TimeUnit.SECONDS)
                assertTrue(requests.all { it.contains("Authorization: Bearer private") })
                assertTrue(requests.last().startsWith("GET /feed?q=one%20two HTTP/1.1"))
            } finally { executor.shutdownNow() }
        }
    }

    @Test fun rejectsHeaderInjectionBeforeConnecting() {
        assertThrows(IllegalArgumentException::class.java) {
            fetch("http://localhost:1/", mapOf("X-Test" to "safe\r\nInjected: yes"))
        }
    }

    private fun fetch(url: String, headers: Map<String, String> = emptyMap()) =
        String(UrlConnectionFetcher(maxBytes = 4).fetch(url, 2_000, headers))

    private fun serve(response: String, block: (String, () -> String) -> Unit) {
        ServerSocket(0, 5, InetAddress.getLoopbackAddress()).use { server ->
            val executor = Executors.newSingleThreadExecutor()
            var request = ""
            val done = executor.submit {
                server.accept().use { socket ->
                    val reader = socket.getInputStream().bufferedReader()
                    request = generateSequence { reader.readLine()?.takeIf { it.isNotEmpty() } }.joinToString("\n")
                    socket.getOutputStream().write(response.toByteArray())
                }
            }
            try {
                block("http://localhost:${server.localPort}/feed", { done.get(5, TimeUnit.SECONDS); request })
                done.get(5, TimeUnit.SECONDS)
            } finally { server.close(); executor.shutdownNow() }
        }
    }
}
