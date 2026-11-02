package app.orchard.tv.live

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.SurfaceView
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import app.orchard.tv.media.MediaContract
import app.orchard.tv.R
import app.orchard.tv.live.model.EpgProgram
import app.orchard.tv.live.playback.LivePlaybackSession
import app.orchard.tv.live.playback.LiveStation
import app.orchard.tv.live.repo.LiveSources
import app.orchard.tv.ui.components.LocalVideoGlass
import app.orchard.tv.ui.components.VideoGlassState
import app.orchard.tv.live.ui.ChannelRow
import app.orchard.tv.live.ui.LiveGlassView
import app.orchard.tv.ui.components.liquidGlass
import app.orchard.tv.ui.components.softEdges
import app.orchard.tv.ui.home.MenuRow
import app.orchard.tv.ui.theme.*
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

/** Foreground-only live player. Back restores Home; no HOME/guard or global codec changes. */
class LiveActivity : ComponentActivity() {
    private var requestSerial by mutableIntStateOf(0)
    private var requestedChannel by mutableStateOf<String?>(null)
    private var active by mutableStateOf(false)
    private var resumeGeneration by mutableIntStateOf(0)
    private var foregroundSession: LivePlaybackSession? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!acceptIntent(intent)) { finish(); return }
        LiveSources.init(this)
        app.orchard.tv.system.Sounds.init(this)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        val reduced = Settings.Global.getFloat(contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
        setContent {
            CompositionLocalProvider(LocalDensity provides Density(canvasDensity(resources.displayMetrics), LocalDensity.current.fontScale),
                LocalReducedMotion provides reduced) {
                LiveScreen(active, resumeGeneration, requestSerial, requestedChannel, onSession = { foregroundSession = it }, onClose = { finish() }, onSettings = {
                    startActivity(Intent(this, LiveSettingsActivity::class.java))
                })
            }
        }
    }
    private fun acceptIntent(value: Intent): Boolean {
        if (value.action != null && value.action != Intent.ACTION_MAIN && value.action != MediaContract.PLAY) return false
        if (value.action == MediaContract.PLAY && value.getIntExtra(MediaContract.EXTRA_VERSION, -1) != MediaContract.VERSION) return false
        requestSerial++
        requestedChannel = MediaContract.channelId(value.getStringExtra(MediaContract.EXTRA_CHANNEL))
        return true
    }
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // Android can route an external NEW_TASK settings request to this existing singleTask root.
        // Verified on Redmi K40S / Android 14; forwarding is action-based, never model-specific.
        if (intent.action == MediaContract.SETTINGS) {
            if (intent.getIntExtra(MediaContract.EXTRA_VERSION, -1) == MediaContract.VERSION)
                startActivity(Intent(this, LiveSettingsActivity::class.java))
            return
        }
        if (acceptIntent(intent)) setIntent(intent)
    }
    override fun onResume() { super.onResume(); resumeGeneration++; active = true }
    override fun onPause() { foregroundSession?.close(); foregroundSession = null; active = false; super.onPause() }
}

@Composable
private fun LiveScreen(active: Boolean, resumeGeneration: Int, requestSerial: Int, requestedChannel: String?, onSession: (LivePlaybackSession?) -> Unit, onClose: () -> Unit, onSettings: () -> Unit) {
    val context = LocalContext.current
    val glass = remember { VideoGlassState() }
    var gpuFailed by remember { mutableStateOf(false) }
    val density = LocalDensity.current.density
    var stations by remember { mutableStateOf<List<LiveStation>>(emptyList()) }
    var programs by remember { mutableStateOf<List<EpgProgram>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var consumedRequest by remember { mutableIntStateOf(-1) }
    var reload by remember { mutableIntStateOf(0) }
    var index by remember { mutableIntStateOf(0) }
    var channels by remember { mutableStateOf(true) }
    var failed by remember { mutableStateOf(false) }
    var buffering by remember { mutableStateOf(false) }
    var session by remember { mutableStateOf<LivePlaybackSession?>(null) }
    val currentSession by rememberUpdatedState(session)
    val latestRequest by rememberUpdatedState(requestSerial to requestedChannel)
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    val prefs = remember { context.getSharedPreferences("live_playback", android.content.Context.MODE_PRIVATE) }
    val first = remember { FocusRequester() }
    val viewport = remember { FocusRequester() }
    val retryFocus = remember { FocusRequester() }
    var viewportFocused by remember { mutableStateOf(false) }
    var hud by remember { mutableStateOf(true) }
    val selected = stations.getOrNull(index)
    LaunchedEffect(reload, active, resumeGeneration) {
        if (!active) return@LaunchedEffect
        loading = true
        stations = withContext(Dispatchers.IO) { val owner = currentCoroutineContext(); runCatching { LiveSources.loadStations { owner.ensureActive() } }.getOrDefault(emptyList()) }
        val request = latestRequest
        val target = request.second.takeIf { consumedRequest != request.first }
        index = app.orchard.tv.live.playback.ChannelSelection.index(stations.map { it.key }, prefs.getString("last_channel", null), target)
        if (stations.isNotEmpty()) {
            if (target != null && stations.getOrNull(index)?.key == target) channels = false
            consumedRequest = request.first
        }
        loading = false
        programs = withContext(Dispatchers.IO) { val owner = currentCoroutineContext(); runCatching { LiveSources.loadPrograms { owner.ensureActive() } }.getOrDefault(emptyList()) }
    }
    LaunchedEffect(requestSerial, stations) {
        if (stations.isNotEmpty() && consumedRequest != requestSerial) {
            requestedChannel?.let { id -> stations.indexOfFirst { it.key == id }.takeIf { it >= 0 }?.let { index = it; channels = false } }
            consumedRequest = requestSerial
        }
    }
    LaunchedEffect(active) { if (active) while (true) { now = System.currentTimeMillis(); delay(30000) } }
    DisposableEffect(active, resumeGeneration, selected) {
        val owned = if (active && selected != null) {
            prefs.edit().putString("last_channel", selected.key).apply()
            LivePlaybackSession(context, selected) { wait, error -> buffering = wait; failed = error }
        } else null
        session = owned; onSession(owned)
        onDispose { owned?.close(); if (session === owned) { session = null; onSession(null) } }
    }
    LaunchedEffect(channels, loading, stations) {
        if (channels && !loading) { withFrameNanos { }; runCatching { first.requestFocus() } }
    }
    LaunchedEffect(channels, index, buffering, failed) {
        hud = true
        if (!channels && !buffering && !failed) { delay(4000); hud = false }
    }
    LaunchedEffect(channels, failed, hud) {
        if (!channels) {
            withFrameNanos { }
            runCatching { (if (failed) retryFocus else viewport).requestFocus() }
        }
    }
    BackHandler { if (channels && selected != null) channels = false else onClose() }
    val list = rememberLazyListState()
    LaunchedEffect(channels, index) { if (channels && stations.isNotEmpty()) list.scrollToItem(index) }
    CompositionLocalProvider(LocalVideoGlass provides glass.takeUnless { gpuFailed }) {
    Box(Modifier.fillMaxSize().background(Color.Black).focusRequester(viewport).onFocusChanged { viewportFocused = it.isFocused }.pointerInput(Unit) {
        detectTapGestures { channels = true }
    }.onPreviewKeyEvent { event ->
        if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
        when (event.nativeKeyEvent.keyCode) {
            android.view.KeyEvent.KEYCODE_MENU -> { if (event.nativeKeyEvent.repeatCount == 0) channels = !channels; true }
            android.view.KeyEvent.KEYCODE_DPAD_CENTER, android.view.KeyEvent.KEYCODE_ENTER ->
                if (!channels && viewportFocused) { channels = true; true } else false
            android.view.KeyEvent.KEYCODE_DPAD_UP, android.view.KeyEvent.KEYCODE_DPAD_DOWN ->
                if (!channels && viewportFocused && stations.isNotEmpty()) { index = (index + (if (event.key == Key.DirectionUp) -1 else 1) + stations.size) % stations.size; true } else false
            else -> false
        }
    }.focusable()) {
        session?.let { owned ->
            if (!gpuFailed) AndroidView(factory = {
                LiveGlassView(it, ready = { surface -> currentSession?.takeUnless { it.isClosed }?.player?.setVideoSurface(surface) }, failed = { gpuFailed = true })
            }, update = { view ->
                if (!owned.isClosed) view.videoSurface?.let { owned.player.setVideoSurface(it) }
                view.aspect(owned.videoAspect)
                view.plate(if (channels || (hud && selected != null) || failed || buffering) glass.bounds else null,
                    density, glass.regular, 1f, .92f, glass.radius)
            }, onRelease = { it.close() }, modifier = Modifier.fillMaxSize())
            else AndroidView(factory = { SurfaceView(it).also { view -> owned.player.setVideoSurfaceView(view) } },
                update = { owned.player.setVideoSurfaceView(it) },
                onRelease = { runCatching { owned.player.clearVideoSurfaceView(it) } }, modifier = Modifier.fillMaxSize())
        }
        if (!loading && stations.isEmpty()) app.orchard.tv.ui.components.GlassSettingsScene {
            Column(Modifier.align(Alignment.Center).width(440.dp).liquidGlass(28.dp).padding(30.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                GlyphIcon(Glyph.Antenna, Color.White, 40.dp)
                BasicText(stringResource(R.string.live_app_name), style = Type.LargeTitle)
                BasicText(stringResource(R.string.live_no_channels), style = Type.Body.copy(color = Palette.SecondaryLabel))
                MenuRow(Glyph.Gear, stringResource(R.string.live_source_settings), onSettings, focusRequester = first)
                MenuRow(Glyph.Play, stringResource(R.string.live_retry), { reload++ })
            }
        }
        if (channels && (loading || stations.isNotEmpty())) Column(Modifier.padding(24.dp).width(324.dp).fillMaxHeight().liquidGlass(28.dp).padding(18.dp)) {
            Row(Modifier.fillMaxWidth().padding(10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                GlyphIcon(Glyph.Antenna, Color.White, 22.dp)
                Column {
                    BasicText(stringResource(R.string.live_channels), style = Type.Title)
                    if (!loading) BasicText(stringResource(R.string.live_channel_count, stations.size), style = Type.Caption)
                }
            }
            if (loading) BasicText(stringResource(R.string.live_loading), style = Type.Body, modifier = Modifier.padding(12.dp))
            else LazyColumn(state = list, modifier = Modifier.weight(1f).softEdges(
                start = { list.canScrollBackward }, end = { list.canScrollForward })) {
                itemsIndexed(stations, key = { _, station -> station.key }) { i, station ->
                    ChannelRow(i + 1, station.channel.name, station.channel.groupTitle, i == index,
                        if (i == index) first else null) { index = i; channels = false }
                }
            }
            if (stations.isNotEmpty()) MenuRow(Glyph.Gear, stringResource(R.string.live_source_settings), onSettings)
        }
        if (!channels && hud && selected != null && !failed && !buffering) Column(Modifier.align(Alignment.BottomStart).padding(28.dp).widthIn(max = 520.dp).liquidGlass(24.dp, regular = false).padding(16.dp)) {
            BasicText(stringResource(R.string.live_now_playing), style = Type.Caption, modifier = Modifier.padding(bottom = 6.dp))
            BasicText(selected.channel.name, style = Type.Title, maxLines = 1)
            val program = programs.firstOrNull { it.channelId == (selected.channel.tvgId ?: selected.channel.id) && now >= it.startMillis && (it.endMillis == null || now < it.endMillis) }
            program?.let {
                BasicText(it.title, style = Type.Callout, maxLines = 1, overflow = TextOverflow.Ellipsis)
                val end = it.endMillis
                if (end != null && end > it.startMillis) Box(Modifier.padding(top = 10.dp).fillMaxWidth().height(3.dp).background(Palette.Fill)) {
                    Box(Modifier.fillMaxWidth(((now - it.startMillis).toFloat() / (end - it.startMillis)).coerceIn(0f, 1f)).height(3.dp).background(Color.White))
                }
            }
            Row {
                Box(Modifier.width(48.dp)) { MenuRow(Glyph.ChevronLeft, "", { index = (index - 1 + stations.size) % stations.size }, modifier = Modifier.semantics { contentDescription = context.getString(R.string.live_previous) }) }
                Box(Modifier.weight(1f)) { MenuRow(Glyph.Antenna, stringResource(R.string.live_channels), { channels = true }) }
                Box(Modifier.width(48.dp)) { MenuRow(Glyph.ChevronRight, "", { index = (index + 1) % stations.size }, modifier = Modifier.semantics { contentDescription = context.getString(R.string.live_next) }) }
            }
        }
        if (!channels && (failed || buffering)) Column(Modifier.align(Alignment.Center).width(280.dp).liquidGlass(24.dp).padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            GlyphIcon(if (failed) Glyph.Offline else Glyph.Antenna, Color.White, 26.dp)
            BasicText(stringResource(if (failed) R.string.live_unavailable else R.string.live_loading), style = Type.Body, modifier = Modifier.padding(12.dp))
            if (failed) {
                MenuRow(Glyph.Play, stringResource(R.string.live_retry), { session?.retry() }, focusRequester = retryFocus)
                MenuRow(Glyph.Antenna, stringResource(R.string.live_channels), { channels = true })
            }
        }
    }
}
}
