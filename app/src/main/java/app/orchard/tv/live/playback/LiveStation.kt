package app.orchard.tv.live.playback

import app.orchard.tv.live.model.Channel
import app.orchard.tv.live.model.LiveSourceConfig
import app.orchard.tv.live.repo.FailoverPlanner
import app.orchard.tv.live.repo.StreamAttempt
import java.net.URI
import java.util.Locale

data class LiveStation(val key: String, val channel: Channel, val attempts: List<StreamAttempt>)

/** Preserve playlist order; matching EPG IDs, or names without an ID, share a fallback chain. */
fun mergeStations(playlists: List<Pair<LiveSourceConfig, List<Channel>>>): List<LiveStation> {
    val ordered = FailoverPlanner.orderSources(playlists.map { it.first })
    val groups = linkedMapOf<String, Pair<Channel, MutableList<StreamAttempt>>>()
    ordered.forEach { source ->
        playlists.firstOrNull { it.first.id == source.id }?.second.orEmpty().forEach { channel ->
            val key = channel.tvgId?.trim()?.takeIf { it.isNotEmpty() }?.let { "id:$it" }
                ?: "name:${channel.name.trim().lowercase(Locale.ROOT)}"
            val group = groups.getOrPut(key) { channel.copy(sourceId = source.id) to mutableListOf() }
            channel.streamUrls.filter { url -> runCatching {
                val uri = URI(url)
                uri.scheme?.lowercase(Locale.ROOT) in listOf("http", "https") && !uri.host.isNullOrEmpty() && uri.rawUserInfo == null
            }.getOrDefault(false) }.forEach { group.second += StreamAttempt(source, it) }
        }
    }
    return groups.mapNotNull { (key, group) ->
        group.second.distinctBy { it.source.id to it.url }.takeIf { it.isNotEmpty() }?.let { LiveStation(key, group.first, it) }
    }
}
