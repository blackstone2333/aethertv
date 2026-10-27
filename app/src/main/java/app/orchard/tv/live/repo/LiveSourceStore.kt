package app.orchard.tv.live.repo

import android.content.Context
import app.orchard.tv.live.model.LiveSourceConfig
import app.orchard.tv.live.model.LiveSourceKind
import app.orchard.tv.live.playback.LiveStation
import app.orchard.tv.live.playback.mergeStations
import app.orchard.tv.live.model.EpgProgram
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.net.URI

/**
 * Device-local IPTV configuration and refresh status. It has no dependency on HomeViewModel: the
 * phone page can configure and refresh feeds while Home remains responsive and entirely offline.
 */
object LiveSources {
    private const val PREFS = "live_sources"
    private const val CONFIGS = "configs"
    private const val CACHE_DIR = "live-feeds"

    private lateinit var app: Context
    private lateinit var preferences: android.content.SharedPreferences
    private lateinit var feeds: LiveFeedRepository
    private val refreshes = LinkedHashMap<String, RefreshState>()

    @Synchronized
    fun init(context: Context) {
        if (::app.isInitialized) return
        app = context.applicationContext
        preferences = app.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        feeds = LiveFeedRepository(LiveCacheStore(File(app.filesDir, CACHE_DIR)))
    }

    @Synchronized
    fun all(): List<LiveSourceConfig> = readConfigs()

    @Synchronized
    fun upsert(config: LiveSourceConfig) {
        validate(config)
        val next = readConfigs().filterNot { it.id == config.id } + config
        writeConfigs(next.sortedWith(compareBy<LiveSourceConfig> { it.priority }.thenBy { it.id }))
    }

    @Synchronized
    fun remove(id: String): Boolean {
        val old = readConfigs()
        val changed = old.any { it.id == id }
        if (changed) writeConfigs(old.filterNot { it.id == id })
        refreshes.remove(id)
        return changed
    }

    /** Refreshes one source synchronously; callers should run it off the main thread. */
    fun refresh(id: String): JSONObject {
        val config = all().firstOrNull { it.id == id }
            ?: return snapshot().put("error", "unknown source")
        val playlist = feeds.loadPlaylist(config, forceRefresh = true)
        var channelCount = 0
        var epgCount = 0
        var fromCache = false
        var stale = false
        var error: String? = null
        when (playlist) {
            is FeedLoad.Success -> {
                channelCount = playlist.value.playlist.channels.size
                fromCache = playlist.fromCache
                stale = playlist.stale
                val epgUrl = config.epgUrl ?: playlist.value.playlist.epgUrl
                if (epgUrl != null) {
                    when (val epg = feeds.loadEpg(config.copy(epgUrl = epgUrl), forceRefresh = true)) {
                        is FeedLoad.Success -> {
                            epgCount = epg.value.programs.size
                            fromCache = fromCache || epg.fromCache
                            stale = stale || epg.stale
                        }
                        is FeedLoad.Failure -> error = epg.error.message
                    }
                }
            }
            is FeedLoad.Failure -> error = playlist.error.message
        }
        synchronized(this) { refreshes[id] = RefreshState(System.currentTimeMillis(), channelCount, epgCount, fromCache, stale, error) }
        return snapshot()
    }

    @Synchronized
    fun snapshot(): JSONObject {
        val sources = JSONArray()
        readConfigs().forEach { config ->
            val status = refreshes[config.id]
            sources.put(JSONObject().apply {
                put("id", config.id)
                put("kind", config.kind.name.lowercase())
                put("m3uUrl", config.m3uUrl)
                config.epgUrl?.let { put("epgUrl", it) }
                put("priority", config.priority)
                put("enabled", config.enabled)
                put("timeoutMillis", config.timeoutMillis)
                put("channels", status?.channels ?: 0)
                put("epgPrograms", status?.epgPrograms ?: 0)
                put("lastRefreshMillis", status?.lastRefreshMillis ?: 0L)
                put("fromCache", status?.fromCache ?: false)
                put("stale", status?.stale ?: false)
                status?.error?.let { put("error", it) }
            })
        }
        return JSONObject().put("sources", sources).put("sourceCount", sources.length())
    }

    /** Worker-thread entry point; networking never holds the configuration/UI monitor. */
    fun loadStations(): List<LiveStation> {
        val playlists = FailoverPlanner.orderSources(all()).mapNotNull { config ->
            when (val loaded = feeds.loadPlaylist(config)) {
                is FeedLoad.Success -> config to loaded.value.playlist.channels
                is FeedLoad.Failure -> null
            }
        }
        return mergeStations(playlists)
    }

    fun loadPrograms(): List<EpgProgram> = FailoverPlanner.orderSources(all()).flatMap { config ->
        val epg = config.epgUrl ?: (feeds.loadPlaylist(config) as? FeedLoad.Success)?.value?.playlist?.epgUrl
        if (epg == null) emptyList() else (feeds.loadEpg(config.copy(epgUrl = epg)) as? FeedLoad.Success)?.value?.programs.orEmpty()
    }.distinctBy { Triple(it.channelId, it.startMillis, it.title) }

    private fun readConfigs(): List<LiveSourceConfig> {
        if (!::preferences.isInitialized) return emptyList()
        val raw = preferences.getString(CONFIGS, "[]") ?: "[]"
        return runCatching {
            val json = JSONArray(raw)
            (0 until json.length()).mapNotNull { i -> json.optJSONObject(i)?.toConfig() }
        }.getOrDefault(emptyList())
    }

    private fun writeConfigs(configs: List<LiveSourceConfig>) {
        val json = JSONArray().apply { configs.forEach { put(it.toJson()) } }
        preferences.edit().putString(CONFIGS, json.toString()).apply()
    }

    private fun validate(config: LiveSourceConfig) {
        require(ID_PATTERN.matches(config.id)) { "invalid source id" }
        require(config.m3uUrl.isHttpUrl()) { "M3U URL must use http or https" }
        config.epgUrl?.let { require(it.isHttpUrl()) { "EPG URL must use http or https" } }
        require(config.timeoutMillis in 1_000..60_000) { "timeout must be between 1000 and 60000 ms" }
    }

    private fun String.isHttpUrl(): Boolean = runCatching {
        val uri = URI(this)
        (uri.scheme.equals("http", true) || uri.scheme.equals("https", true)) && !uri.host.isNullOrBlank()
    }.getOrDefault(false)

    private fun LiveSourceConfig.toJson() = JSONObject().apply {
        put("id", id); put("kind", kind.name); put("m3uUrl", m3uUrl)
        epgUrl?.let { put("epgUrl", it) }
        put("priority", priority); put("enabled", enabled); put("timeoutMillis", timeoutMillis)
        put("headers", JSONObject(headers))
    }

    private fun JSONObject.toConfig(): LiveSourceConfig? = runCatching {
        val headersObject = optJSONObject("headers")
        val headers = buildMap {
            if (headersObject != null) headersObject.keys().forEach { key -> put(key, headersObject.optString(key)) }
        }
        LiveSourceConfig(
            id = getString("id"),
            kind = LiveSourceKind.of(optString("kind")),
            m3uUrl = getString("m3uUrl"),
            epgUrl = optString("epgUrl").takeIf { it.isNotBlank() },
            priority = optInt("priority", 0),
            enabled = optBoolean("enabled", true),
            timeoutMillis = optInt("timeoutMillis", 8_000),
            headers = headers,
        ).also(::validate)
    }.getOrNull()

    private data class RefreshState(
        val lastRefreshMillis: Long,
        val channels: Int,
        val epgPrograms: Int,
        val fromCache: Boolean,
        val stale: Boolean,
        val error: String?,
    )

    private val ID_PATTERN = Regex("[A-Za-z0-9][A-Za-z0-9._-]{0,63}")
}
