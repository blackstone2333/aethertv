package app.orchard.tv.live.repo

import app.orchard.tv.live.model.Channel
import app.orchard.tv.live.model.LiveSourceConfig
import app.orchard.tv.live.model.LiveSourceKind

data class StreamAttempt(
    val source: LiveSourceConfig,
    val url: String,
)

/** Produces a deterministic attempt order; playback decides when an attempt has failed. */
object FailoverPlanner {
    fun orderSources(
        sources: Collection<LiveSourceConfig>,
        failedSourceIds: Set<String> = emptySet(),
        preferIpv6: Boolean = false,
    ): List<LiveSourceConfig> = sources
        .asSequence()
        .filter { it.enabled && it.id !in failedSourceIds }
        .sortedWith(compareBy<LiveSourceConfig>({ it.priority }, { kindRank(it.kind, preferIpv6) }, { it.id }))
        .toList()

    /** [streamUrlsBySource] is normally assembled by merging the playlists of each source. */
    fun plan(
        channel: Channel,
        sources: Collection<LiveSourceConfig>,
        streamUrlsBySource: Map<String, List<String>> = emptyMap(),
        failedSourceIds: Set<String> = emptySet(),
        preferIpv6: Boolean = false,
    ): List<StreamAttempt> {
        val ordered = orderSources(sources, failedSourceIds, preferIpv6)
        val attempts = ArrayList<StreamAttempt>()
        ordered.forEach { source ->
            val urls = streamUrlsBySource[source.id]
                ?: if (source.id == channel.sourceId) channel.streamUrls else emptyList()
            urls.filter { it.isNotBlank() }.forEach { url -> attempts += StreamAttempt(source, url) }
        }
        return attempts.distinctBy { it.source.id to it.url }
    }

    private fun kindRank(kind: LiveSourceKind, preferIpv6: Boolean): Int = when {
        preferIpv6 && kind == LiveSourceKind.Ipv6 -> 0
        kind == LiveSourceKind.Lan -> 0
        kind == LiveSourceKind.Custom -> 1
        kind == LiveSourceKind.Public -> 2
        else -> 3
    }
}
