package app.orchard.tv.live.playback

/** Resolve a known opaque request before allocating a decoder; unknown IDs retain the last channel. */
object ChannelSelection {
    fun index(keys: List<String>, last: String?, requested: String?): Int {
        val explicit = keys.indexOfFirst { it == requested }
        return if (explicit >= 0) explicit else keys.indexOfFirst { it == last }.coerceAtLeast(0)
    }
}
