package app.orchard.tv.media

import android.content.Context
import android.content.Intent

/** Explicit foreground handoff. Home owns neither channel databases nor a live decoder. */
object MediaBridge {
    fun liveIntent(context: Context, settings: Boolean = false): Intent? {
        val intent = Intent(if (settings) MediaContract.SETTINGS else MediaContract.PLAY)
            .setPackage(MediaContract.LIVE_PACKAGE).putExtra(MediaContract.EXTRA_VERSION, MediaContract.VERSION)
        return intent.takeIf { it.resolveActivity(context.packageManager) != null }
    }
}
