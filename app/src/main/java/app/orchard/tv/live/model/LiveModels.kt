package app.orchard.tv.live.model

/** A source kind is only a routing hint; [LiveSourceConfig.priority] is authoritative. */
enum class LiveSourceKind {
    Lan,
    Public,
    Ipv6,
    Custom;

    companion object {
        fun of(value: String?): LiveSourceKind = entries.firstOrNull {
            it.name.equals(value?.trim(), ignoreCase = true)
        } ?: Custom
    }
}

data class LiveSourceConfig(
    val id: String,
    val kind: LiveSourceKind = LiveSourceKind.Lan,
    val m3uUrl: String,
    val epgUrl: String? = null,
    val priority: Int = 0,
    val enabled: Boolean = true,
    val timeoutMillis: Int = 8_000,
    val headers: Map<String, String> = emptyMap(),
)

/** A channel can contain more than one URL when an M3U contains duplicate entries. */
data class Channel(
    val id: String,
    val name: String,
    val tvgId: String? = null,
    val tvgName: String? = null,
    val logoUrl: String? = null,
    val groupTitle: String? = null,
    val streamUrls: List<String> = emptyList(),
    val sourceId: String? = null,
)

data class EpgProgram(
    val channelId: String,
    val startMillis: Long,
    val endMillis: Long?,
    val title: String,
    val subtitle: String? = null,
    val description: String? = null,
)

data class M3uPlaylist(
    val channels: List<Channel>,
    val epgUrl: String? = null,
)

sealed interface LivePlaybackState {
    data object Idle : LivePlaybackState
    data class Loading(val channelId: String) : LivePlaybackState
    data class Playing(val channelId: String, val streamUrl: String) : LivePlaybackState
    data class Buffering(val channelId: String, val streamUrl: String) : LivePlaybackState
    data class Failed(val channelId: String?, val message: String, val recoverable: Boolean = true) : LivePlaybackState
}
