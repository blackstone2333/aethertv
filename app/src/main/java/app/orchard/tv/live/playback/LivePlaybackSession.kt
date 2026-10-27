package app.orchard.tv.live.playback

import android.content.Context
import android.os.Handler
import android.os.Looper
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.datasource.DataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import app.orchard.tv.system.DiagLog

/** One foreground decoder; attempts are bounded and never reopen a failed URL in a loop. */
class LivePlaybackSession(context: Context, private val station: LiveStation,
                          private val update: (buffering: Boolean, failed: Boolean) -> Unit) {
    val player = ExoPlayer.Builder(context).setLoadControl(DefaultLoadControl.Builder()
        .setBufferDurationsMs(1500, 12000, 500, 1000).build()).build()
    private val handler = Handler(Looper.getMainLooper())
    private var attempt = -1
    private var closed = false
    private var rendered = false
    private val timeout = Runnable { next() }

    init {
        player.addListener(object : Player.Listener {
            override fun onPlayerError(error: PlaybackException) { next() }
            override fun onRenderedFirstFrame() {
                rendered = true; handler.removeCallbacks(timeout); update(false, false)
                DiagLog.i("live playback rendered first frame (attempt ${attempt + 1})")
            }
            override fun onPlaybackStateChanged(state: Int) {
                if (closed) return
                when (state) {
                    Player.STATE_BUFFERING -> { update(true, false); armTimeout() }
                    Player.STATE_READY -> { update(false, false); if (rendered) handler.removeCallbacks(timeout) }
                    Player.STATE_ENDED -> next()
                }
            }
        })
        next()
    }

    fun retry() { if (!closed) { attempt = -1; next() } }

    private fun armTimeout() {
        handler.removeCallbacks(timeout)
        handler.postDelayed(timeout, station.attempts.getOrNull(attempt)?.source?.timeoutMillis?.toLong() ?: 8000L)
    }

    private fun next() {
        if (closed) return
        handler.removeCallbacks(timeout)
        player.stop()
        val target = station.attempts.getOrNull(++attempt)
        if (target == null) { update(false, true); return }
        rendered = false; update(true, false)
        val factory = DataSource.Factory { LiveStreamDataSource(target.url, target.source.headers, target.source.timeoutMillis) }
        player.setMediaSource(DefaultMediaSourceFactory(factory).createMediaSource(MediaItem.fromUri(target.url)))
        player.volume = 1f
        player.prepare(); player.playWhenReady = true
        armTimeout()
        DiagLog.i("live playback opens attempt ${attempt + 1}/${station.attempts.size}")
    }

    fun close() {
        if (closed) return
        closed = true; handler.removeCallbacksAndMessages(null)
        player.release()
        DiagLog.i("live playback released decoder")
    }
}
