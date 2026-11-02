package app.orchard.tv.media

/** Public launch contract: only opaque channel IDs cross app boundaries, never stream credentials. */
object MediaContract {
    const val VERSION = 1
    const val LIVE_PACKAGE = "app.aethertv.live"
    const val PLAY = "$LIVE_PACKAGE.PLAY"
    const val SETTINGS = "$LIVE_PACKAGE.SETTINGS"
    const val EXTRA_VERSION = "contract_version"
    const val EXTRA_CHANNEL = "channel_id"
    const val MIGRATION_AUTHORITY = "com.dangbei.tvlauncher.media-config"
    const val MIGRATION_PERMISSION = "com.dangbei.tvlauncher.permission.MEDIA_CONFIG"
    fun channelId(value: String?): String? = value?.takeIf { it.length in 1..256 && it.none { c -> c.isISOControl() } }
}
