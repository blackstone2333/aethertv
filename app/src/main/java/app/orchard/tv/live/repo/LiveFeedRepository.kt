package app.orchard.tv.live.repo

import app.orchard.tv.live.model.LiveSourceConfig
import app.orchard.tv.live.model.EpgProgram
import app.orchard.tv.live.model.M3uPlaylist
import app.orchard.tv.live.parser.M3uParser
import app.orchard.tv.live.parser.XmlTvParser
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

data class FeedError(val message: String, val cause: Throwable? = null)

data class PlaylistResult(
    val playlist: M3uPlaylist,
    val fromCache: Boolean,
    val stale: Boolean,
)

data class EpgResult(
    val programs: List<EpgProgram>,
    val fromCache: Boolean,
    val stale: Boolean,
)

sealed interface FeedLoad<out T> {
    data class Success<T>(val value: T, val fromCache: Boolean, val stale: Boolean) : FeedLoad<T>
    data class Failure(val error: FeedError) : FeedLoad<Nothing>
}

fun interface LiveHttpFetcher {
    fun fetch(url: String, timeoutMillis: Int, headers: Map<String, String>): ByteArray
}

class UrlConnectionFetcher(private val maxBytes: Long = 64L * 1024 * 1024) : LiveHttpFetcher {
    override fun fetch(url: String, timeoutMillis: Int, headers: Map<String, String>): ByteArray {
        val connection = URL(url).openConnection() as? HttpURLConnection
            ?: throw IOException("unsupported URL: $url")
        try {
            connection.connectTimeout = timeoutMillis.coerceAtLeast(1)
            connection.readTimeout = timeoutMillis.coerceAtLeast(1)
            connection.instanceFollowRedirects = true
            connection.useCaches = false
            headers.forEach { (key, value) -> connection.setRequestProperty(key, value) }
            val code = connection.responseCode
            if (code !in 200..299) throw IOException("HTTP $code for $url")
            val declared = connection.contentLengthLong
            if (declared > maxBytes) throw IOException("response too large")
            return connection.inputStream.use { it.readBounded(maxBytes) }
        } finally {
            connection.disconnect()
        }
    }

    private fun java.io.InputStream.readBounded(limit: Long): ByteArray {
        val out = ByteArrayOutputStream()
        val buffer = ByteArray(8192)
        var total = 0L
        while (true) {
            val count = read(buffer)
            if (count < 0) break
            total += count
            if (total > limit) throw IOException("response too large")
            out.write(buffer, 0, count)
        }
        return out.toByteArray()
    }
}

/** Fetches feeds on a worker thread chosen by the caller and falls back to stale disk data. */
class LiveFeedRepository(
    private val cache: LiveCacheStore,
    private val fetcher: LiveHttpFetcher = UrlConnectionFetcher(),
    private val m3uParser: M3uParser = M3uParser(),
    private val epgParser: XmlTvParser = XmlTvParser(),
    private val nowMillis: () -> Long = { System.currentTimeMillis() },
) {
    fun loadPlaylist(config: LiveSourceConfig, forceRefresh: Boolean = false): FeedLoad<PlaylistResult> =
        loadBytes("${config.id}:m3u:${config.m3uUrl}", config.m3uUrl, config, forceRefresh) { bytes ->
            m3uParser.parse(bytes.inputStream())
        }.map { playlist, fromCache, stale -> PlaylistResult(playlist, fromCache, stale) }

    fun loadEpg(config: LiveSourceConfig, forceRefresh: Boolean = false): FeedLoad<EpgResult> {
        val url = config.epgUrl?.takeIf { it.isNotBlank() }
            ?: return FeedLoad.Failure(FeedError("EPG URL is not configured"))
        return loadBytes("${config.id}:epg:$url", url, config, forceRefresh) { bytes ->
            epgParser.parseAuto(bytes.inputStream())
        }.map { programs, fromCache, stale -> EpgResult(programs, fromCache, stale) }
    }

    private fun <T> loadBytes(
        key: String,
        url: String,
        config: LiveSourceConfig,
        forceRefresh: Boolean,
        parse: (ByteArray) -> T,
    ): FeedLoad<T> {
        val cached = cache.read(key)
        if (!forceRefresh && cached?.fresh == true) {
            runCatching { parse(cached.bytes) }.getOrNull()?.let { return FeedLoad.Success(it, true, false) }
        }
        val network = runCatching { fetcher.fetch(url, config.timeoutMillis, config.headers) }
            .mapCatching { bytes -> parse(bytes).also { cache.write(key, bytes, nowMillis()) } }
        network.getOrNull()?.let { return FeedLoad.Success(it, false, false) }
        cached?.let { runCatching { parse(it.bytes) }.getOrNull()?.let { value -> return FeedLoad.Success(value, true, true) } }
        return FeedLoad.Failure(FeedError("Feed unavailable: $url", network.exceptionOrNull()))
    }

    private inline fun <T, R> FeedLoad<T>.map(transform: (T, Boolean, Boolean) -> R): FeedLoad<R> = when (this) {
        is FeedLoad.Success -> FeedLoad.Success(transform(value, fromCache, stale), fromCache, stale)
        is FeedLoad.Failure -> this
    }
}
