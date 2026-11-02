package app.orchard.tv.live.repo

import app.orchard.tv.live.model.LiveSourceConfig
import app.orchard.tv.live.model.EpgProgram
import app.orchard.tv.live.model.M3uPlaylist
import app.orchard.tv.live.parser.M3uParser
import app.orchard.tv.live.parser.XmlTvParser

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
