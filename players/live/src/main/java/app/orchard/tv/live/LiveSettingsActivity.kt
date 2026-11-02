package app.orchard.tv.live

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.*
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import app.orchard.tv.R
import app.orchard.tv.live.model.LiveSourceConfig
import app.orchard.tv.live.repo.LiveSources
import app.orchard.tv.media.MediaContract
import app.orchard.tv.ui.components.liquidGlass
import app.orchard.tv.ui.home.MenuRow
import app.orchard.tv.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID

/** User-owned configuration; migration never removes or replaces an existing source. */
class LiveSettingsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (intent.action != null && (intent.action != MediaContract.SETTINGS ||
                intent.getIntExtra(MediaContract.EXTRA_VERSION, -1) != MediaContract.VERSION)) { finish(); return }
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
        LiveSources.init(this)
        app.orchard.tv.system.Sounds.init(this)
        val reduced = android.provider.Settings.Global.getFloat(contentResolver, android.provider.Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
        setContent {
            CompositionLocalProvider(LocalDensity provides Density(canvasDensity(resources.displayMetrics), LocalDensity.current.fontScale), LocalReducedMotion provides reduced) {
                SourceSettings(onPlay = {
                    startActivity(Intent(this, LiveActivity::class.java).setAction(MediaContract.PLAY)
                        .putExtra(MediaContract.EXTRA_VERSION, MediaContract.VERSION))
                    finish()
                }, importLegacy = {
                    val exported = contentResolver.call(Uri.parse("content://${MediaContract.MIGRATION_AUTHORITY}"), "export_live_v1", null, null)
                        ?: error("export unavailable")
                    require(exported.getInt("version") == MediaContract.VERSION)
                    LiveSources.importMissing(exported.getString("configs") ?: error("missing configuration"))
                })
            }
        }
    }
}

@Composable
private fun SourceSettings(onPlay: () -> Unit, importLegacy: () -> Int) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var sources by remember { mutableStateOf(LiveSources.all()) }
    var editing by remember { mutableStateOf<LiveSourceConfig?>(null) }
    var showEditor by remember { mutableStateOf(false) }
    var label by remember { mutableStateOf("") }
    var url by remember { mutableStateOf("") }
    var enabled by remember { mutableStateOf(true) }
    var epg by remember { mutableStateOf("") }
    var message by remember { mutableStateOf<String?>(null) }
    var importing by remember { mutableStateOf(false) }
    var removing by remember { mutableStateOf(false) }
    val primary = remember { FocusRequester() }
    val nameFocus = remember { FocusRequester() }
    val addFocus = remember { FocusRequester() }
    val sourceFocus = remember { mutableMapOf<String, FocusRequester>() }
    var returnFocus by remember { mutableStateOf(primary) }
    fun edit(source: LiveSourceConfig?, origin: FocusRequester = primary) {
        returnFocus = origin
        editing = source; label = source?.label.orEmpty(); url = source?.m3uUrl.orEmpty()
        epg = source?.epgUrl.orEmpty(); enabled = source?.enabled ?: true; showEditor = true; removing = false; message = null
    }
    fun closeEditor() { showEditor = false; editing = null; removing = false; message = null }
    androidx.activity.compose.BackHandler(showEditor) { closeEditor() }
    LaunchedEffect(showEditor) {
        withFrameNanos { }; runCatching { (if (showEditor) nameFocus else returnFocus).requestFocus() }
    }
    LaunchedEffect(importing) {
        if (importing) {
            val result = withContext(Dispatchers.IO) { runCatching { importLegacy() } }
            message = result.fold({ count -> if (count == 0) context.getString(R.string.live_import_unchanged)
                else context.getString(R.string.live_imported_count, count) }, { context.getString(R.string.live_settings_failed) })
            sources = LiveSources.all(); importing = false
        }
    }
    app.orchard.tv.ui.components.GlassSettingsScene {
        Column(Modifier.fillMaxSize().padding(horizontal = 36.dp, vertical = 24.dp)) {
            BasicText(stringResource(R.string.live_app_name), style = Type.LargeTitle)
            BasicText(stringResource(R.string.live_source_settings), style = Type.Callout, modifier = Modifier.padding(top = 4.dp, bottom = 18.dp))
            Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(22.dp)) {
                Column(Modifier.width(272.dp).fillMaxHeight().liquidGlass(28.dp).padding(16.dp)) {
                    BasicText(stringResource(R.string.live_playlists), style = Type.Headline, modifier = Modifier.padding(10.dp))
                    val scroll = rememberScrollState()
                    Column(Modifier.weight(1f).verticalScroll(scroll)) {
                        sources.forEach { source ->
                            val requester = sourceFocus.getOrPut(source.id) { FocusRequester() }
                            MenuRow(Glyph.Antenna, source.label ?: source.id, { edit(source, requester) }, focusRequester = requester, selected = showEditor && editing?.id == source.id, trailing = if (source.enabled) "✓" else null)
                        }
                        MenuRow(Glyph.ChevronRight, stringResource(R.string.live_add_source), { edit(null, addFocus) }, focusRequester = addFocus)
                    }
                    androidx.compose.foundation.Canvas(Modifier.fillMaxWidth().height(1.dp)) { drawLine(Palette.Hairline, androidx.compose.ui.geometry.Offset.Zero, androidx.compose.ui.geometry.Offset(size.width, 0f)) }
                    MenuRow(Glyph.Download, stringResource(R.string.live_import), { if (!importing) importing = true })
                    BasicText(stringResource(R.string.live_import_hint), style = Type.Caption, modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp))
                }
                Column(Modifier.weight(1f).fillMaxHeight().liquidGlass(28.dp).padding(24.dp)) {
                    if (!showEditor) {
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.Center) {
                            GlyphIcon(Glyph.Antenna, Color.White.copy(alpha = .9f), 40.dp)
                            BasicText(stringResource(if (sources.isEmpty()) R.string.live_setup_title else R.string.live_ready_title), style = Type.LargeTitle, modifier = Modifier.padding(top = 18.dp, bottom = 10.dp))
                            BasicText(stringResource(if (sources.isEmpty()) R.string.live_setup_hint else R.string.live_ready_hint), style = Type.Body.copy(color = Palette.SecondaryLabel), modifier = Modifier.padding(bottom = 20.dp))
                            MenuRow(if (sources.isEmpty()) Glyph.ChevronRight else Glyph.Play,
                                stringResource(if (sources.isEmpty()) R.string.live_add_source else R.string.live_watch),
                                { if (sources.isEmpty()) edit(null) else onPlay() }, focusRequester = primary)
                            BasicText(stringResource(R.string.live_formats_hint), style = Type.Caption, modifier = Modifier.padding(top = 16.dp, start = 12.dp, end = 12.dp))
                        }
                    } else {
                        BasicText(stringResource(if (editing == null) R.string.live_add_source else R.string.live_edit_source), style = Type.Title)
                        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                            SourceField(stringResource(R.string.live_source_name), label, nameFocus) { if (it.length <= 64) label = it }
                            SourceField(stringResource(R.string.live_playlist_url), url) { url = it }
                            SourceField(stringResource(R.string.live_epg_url), epg) { epg = it }
                            editing?.let { source ->
                                MenuRow(Glyph.Gear, stringResource(R.string.live_enabled), {
                                    enabled = !enabled
                                }, trailing = if (enabled) "✓" else null)
                            }
                            Spacer(Modifier.height(12.dp))
                            editing?.let { source ->
                                MenuRow(Glyph.Trash, stringResource(if (removing) R.string.live_remove_confirm else R.string.live_remove), {
                                    if (removing) {
                                        LiveSources.remove(source.id); sources = LiveSources.all(); returnFocus = primary; closeEditor()
                                    } else removing = true
                                }, destructive = true)
                            }
                        }
                        Row(Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Box(Modifier.weight(1f).background(Palette.Fill, RoundedCornerShape(12.dp))) { MenuRow(Glyph.Check, stringResource(R.string.live_save), {
                                val result = runCatching {
                                    LiveSources.upsert((editing ?: LiveSourceConfig(id = UUID.randomUUID().toString(), m3uUrl = url.trim()))
                                        .copy(enabled = enabled, label = label.trim().ifEmpty { java.net.URI(url.trim()).host }, m3uUrl = url.trim(), epgUrl = epg.trim().ifEmpty { null }))
                                }
                                if (result.isSuccess) {
                                    sources = LiveSources.all(); closeEditor(); message = context.getString(R.string.live_saved)
                                } else message = context.getString(R.string.live_settings_failed)
                            }) }
                            Box(Modifier.weight(1f)) { MenuRow(Glyph.ChevronLeft, stringResource(R.string.live_cancel), { closeEditor() }) }
                        }
                    }
                    if (importing) BasicText(stringResource(R.string.live_importing), style = Type.Callout, modifier = Modifier.padding(top = 8.dp))
                    message?.let { BasicText(it, style = Type.Callout, modifier = Modifier.padding(top = 8.dp)) }
                }
            }
            BasicText(stringResource(R.string.live_settings_navigation), style = Type.Caption, modifier = Modifier.padding(top = 12.dp))
        }
    }
}

@Composable
private fun SourceField(label: String, value: String, requester: FocusRequester? = null, change: (String) -> Unit) {
    var focused by remember { mutableStateOf(false) }
    BasicText(label, style = Type.Callout, modifier = Modifier.padding(top = 16.dp, bottom = 8.dp))
    BasicTextField(value, { if (it.length <= 8192) change(it) }, singleLine = true,
        textStyle = Type.Body, cursorBrush = SolidColor(Color.White),
        modifier = Modifier.fillMaxWidth().semantics { contentDescription = label }.then(if (requester != null) Modifier.focusRequester(requester) else Modifier)
            .onFocusChanged { focused = it.isFocused }
            .border(if (focused) 2.dp else 1.dp, if (focused) Palette.Focus else Palette.Hairline, RoundedCornerShape(10.dp))
            .background(Palette.Fill, RoundedCornerShape(10.dp)).padding(12.dp))
}
