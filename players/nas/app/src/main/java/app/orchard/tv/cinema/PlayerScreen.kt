// SPDX-License-Identifier: GPL-2.0-only
package app.orchard.tv.cinema

import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.*
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.PlayerView
import app.orchard.tv.R
import app.orchard.tv.ui.theme.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel

/** The queue survives screen disposal long enough to deliver the final stop in order. */
private object Reports {
    private val scope=CoroutineScope(SupervisorJob()+Dispatchers.IO)
    private val queue=Channel<suspend () -> Unit>(32)
    init { scope.launch { for (work in queue) runCatching { work() } } }
    fun send(work: suspend () -> Unit) { queue.trySend(work) }
}
@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
@Composable fun PlayerScreen(model: CinemaModel,p: Playback) {
    val context=LocalContext.current
    val a=model.account ?: return
    var position by remember(p) { mutableLongStateOf(p.startTicks/10000) }
    var duration by remember(p) { mutableLongStateOf(p.item.ticks/10000) }
    var paused by remember(p) { mutableStateOf(false) }
    var trackMenu by remember(p) { mutableIntStateOf(0) }
    var tracks by remember(p) { mutableStateOf(Tracks.EMPTY) }
    var failed by remember(p) { mutableStateOf(false) }
    var controls by remember(p) {mutableStateOf(true)}
    var lastInput by remember(p) {mutableLongStateOf(android.os.SystemClock.elapsedRealtime())}
    var status by remember(p) { mutableIntStateOf(Player.STATE_BUFFERING) }
    val playFocus=remember { FocusRequester() }
    val player=remember(p) {
        val http=DefaultHttpDataSource.Factory().setUserAgent("AetherCinema/0.1.0").setConnectTimeoutMs(8000).setReadTimeoutMs(15000).setAllowCrossProtocolRedirects(false)
        ExoPlayer.Builder(context).setMediaSourceFactory(DefaultMediaSourceFactory(DefaultDataSource.Factory(context,http)))
            .setLoadControl(DefaultLoadControl.Builder().setBufferDurationsMs(12000,30000,1000,2000).setTargetBufferBytes(16*1024*1024).setPrioritizeTimeOverSizeThresholds(false).build()).build().apply {
                val subs=p.subtitles.map { sub -> MediaItem.SubtitleConfiguration.Builder(android.net.Uri.parse(sub.url)).setId(sub.index.toString()).setLabel(sub.label).setLanguage(sub.language).setMimeType(MimeTypes.TEXT_VTT).build() }
                setMediaItem(MediaItem.Builder().setUri(p.url).setSubtitleConfigurations(subs).build(),if(p.method=="DirectPlay") p.startTicks/10000 else 0)
                trackSelectionParameters=trackSelectionParameters.buildUpon().setTrackTypeDisabled(C.TRACK_TYPE_TEXT,p.subtitle==null).build()
                setHandleAudioBecomingNoisy(true)
                prepare(); playWhenReady=true
            }
    }
    fun report(event: String,ticks: Long,isPaused: Boolean,hasFailed: Boolean=false) { Reports.send { provider(a.backend).report(a,p.copy(item=p.item.copy(ticks=duration*10000)),event,ticks,isPaused,hasFailed) } }
    var finished by remember(p) { mutableStateOf(false) }
    var started by remember(p) { mutableStateOf(false) }
    fun finish() {
        if(finished) return
        finished=true
        val ticks=((player.currentPosition.coerceAtLeast(0)+p.offsetMillis)*10000).coerceAtMost(p.item.ticks.takeIf{it>0} ?: Long.MAX_VALUE)
        model.stopped(p.copy(item=p.item.copy(ticks=duration*10000)),ticks)
        if(started) report("Playing/Stopped",ticks,true,failed)
        player.release()
    }
    DisposableEffect(player) {
        val listener=object: Player.Listener {
            override fun onTracksChanged(value: Tracks) { tracks=value }
            override fun onIsPlayingChanged(isPlaying: Boolean) { paused=!isPlaying; if(started && !finished) report("Playing/Progress",(player.currentPosition+p.offsetMillis)*10000,!isPlaying) }
            override fun onPlaybackStateChanged(state: Int) {
                status=state
                if(state==Player.STATE_READY && !started) { started=true; report("Playing",p.startTicks,false) }
                if(state==Player.STATE_ENDED) { finish(); model.playback=null }
            }
            override fun onPlayerError(error: PlaybackException) { failed=true; model.error="media"; finish(); model.playback=null }
        }
        player.addListener(listener)
        model.stopPlayer={finish()}
        onDispose { finish(); player.removeListener(listener); model.stopPlayer=null }
    }
    LaunchedEffect(player) {
        runCatching {playFocus.requestFocus()}
        var count=0
        while(isActive && !finished) {
            delay(1000)
            position=player.currentPosition.coerceAtLeast(0)+p.offsetMillis
            duration=player.duration.takeIf{it>0}?.plus(p.offsetMillis) ?: p.item.ticks/10000
            if(player.isPlaying && trackMenu==0 && android.os.SystemClock.elapsedRealtime()-lastInput>5000) controls=false
            count++
            if(count%10==0 && started) {
                model.resumes.save(a,p.item.id,position*10000)
                report("Playing/Progress",position*10000,!player.isPlaying)
            }
        }
    }
    Box(Modifier.fillMaxSize().background(Color.Black).pointerInput(Unit) {detectTapGestures {controls=true;lastInput=android.os.SystemClock.elapsedRealtime()}}.onPreviewKeyEvent { e ->
        lastInput=android.os.SystemClock.elapsedRealtime()
        if(!controls && e.nativeKeyEvent.keyCode in listOf(android.view.KeyEvent.KEYCODE_DPAD_CENTER,android.view.KeyEvent.KEYCODE_ENTER,android.view.KeyEvent.KEYCODE_DPAD_LEFT,android.view.KeyEvent.KEYCODE_DPAD_RIGHT,android.view.KeyEvent.KEYCODE_DPAD_UP,android.view.KeyEvent.KEYCODE_DPAD_DOWN)) {
            controls=true
            true
        } else if(e.type==KeyEventType.KeyUp && e.nativeKeyEvent.keyCode in listOf(android.view.KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE,android.view.KeyEvent.KEYCODE_MEDIA_PLAY,android.view.KeyEvent.KEYCODE_MEDIA_PAUSE)) {
            controls=true; if(player.isPlaying) player.pause() else player.play(); true
        } else false
    }) {
        AndroidView(factory={PlayerView(it).apply { useController=false; this.player=player; layoutParams=ViewGroup.LayoutParams(-1,-1) }},modifier=Modifier.fillMaxSize(),onRelease={it.player=null})
        Column(Modifier.align(Alignment.BottomCenter).fillMaxWidth().graphicsLayer {alpha=if(controls || paused || status==Player.STATE_BUFFERING || trackMenu!=0) 1f else 0f}.padding(28.dp).clip(RoundedCornerShape(22.dp)).background(Color(0xCF171B23)).padding(18.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) {
                Label(p.item.title)
                Label(if(status==Player.STATE_BUFFERING) stringResource(R.string.loading) else stringResource(if(p.method=="DirectPlay") R.string.direct_play else R.string.server_conversion),style=Type.Caption)
            }
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(12.dp),verticalAlignment=Alignment.CenterVertically) {
                Label(formatMillis(position),style=Type.Caption)
                Box(Modifier.weight(1f).height(4.dp).background(Color.DarkGray)) { Box(Modifier.fillMaxWidth(if(duration>0) (position.toFloat()/duration).coerceIn(0f,1f) else 0f).fillMaxHeight().background(Color.White)) }
                Label(formatMillis(duration),style=Type.Caption)
            }
            Row(horizontalArrangement=Arrangement.spacedBy(10.dp)) {
                Action(stringResource(if(paused) R.string.play else R.string.pause),{if(player.isPlaying) player.pause() else player.play()},requester=playFocus)
                Action(stringResource(R.string.seek_back),{player.seekTo((player.currentPosition-10000).coerceAtLeast(0))})
                Action(stringResource(R.string.seek_forward),{player.seekTo((player.currentPosition+30000).coerceAtMost(player.duration.coerceAtLeast(0)))})
                Action(stringResource(R.string.audio),{trackMenu=C.TRACK_TYPE_AUDIO})
                Action(stringResource(R.string.subtitles),{trackMenu=C.TRACK_TYPE_TEXT})
                Action(stringResource(R.string.stop),{finish();model.playback=null})
            }
        }
        if(trackMenu!=0) {
            val first=remember(trackMenu) {FocusRequester()}
            androidx.activity.compose.BackHandler {trackMenu=0}
            LaunchedEffect(trackMenu) {first.requestFocus()}
            LazyColumn(Modifier.align(Alignment.CenterEnd).padding(30.dp).width(330.dp).heightIn(max=390.dp).clip(RoundedCornerShape(20.dp)).background(Color(0xFA242832)).padding(20.dp),verticalArrangement=Arrangement.spacedBy(14.dp)) {
                item { Label(stringResource(if(trackMenu==C.TRACK_TYPE_AUDIO) R.string.audio else R.string.subtitles),style=Type.Title) }
                item { Action(stringResource(if(trackMenu==C.TRACK_TYPE_TEXT) R.string.off else R.string.automatic),{
                    player.trackSelectionParameters=player.trackSelectionParameters.buildUpon().clearOverridesOfType(trackMenu).setTrackTypeDisabled(trackMenu,trackMenu==C.TRACK_TYPE_TEXT).build(); trackMenu=0
                },requester=first) }
                if(a.backend!=Backend.MOONTV) p.serverTracks.filter {it.type==if(trackMenu==C.TRACK_TYPE_AUDIO) "Audio" else "Subtitle"}.forEach { remote ->
                    item { Action(stringResource(R.string.server_track,remote.label),{
                        val ticks=(player.currentPosition+p.offsetMillis)*10000
                        if(trackMenu==C.TRACK_TYPE_AUDIO) model.changeServerTrack(p,ticks,audio=remote.index)
                        else model.changeServerTrack(p,ticks,subtitle=remote.index)
                        trackMenu=0
                    }) }
                }
                tracks.groups.filter{it.type==trackMenu}.forEach { group ->
                    (0 until group.length).forEach { i -> if(group.isTrackSupported(i)) item {
                        val f=group.getTrackFormat(i)
                        Action(f.label ?: f.language ?: stringResource(R.string.track_number,i+1),{
                            player.trackSelectionParameters=player.trackSelectionParameters.buildUpon().setTrackTypeDisabled(trackMenu,false).setOverrideForType(TrackSelectionOverride(group.mediaTrackGroup,listOf(i))).build(); trackMenu=0
                        })
                    } }
                }
            }
        }
    }
}
