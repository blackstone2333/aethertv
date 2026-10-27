package app.orchard.tv.ui.control

import android.content.Intent
import android.provider.OpenableColumns
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import app.orchard.tv.ui.components.softScrollEdges
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.key.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.orchard.tv.CcPage
import app.orchard.tv.R
import app.orchard.tv.system.SystemMonitor
import app.orchard.tv.system.TransferStore
import app.orchard.tv.ui.components.Pressable
import app.orchard.tv.ui.home.MenuRow
import app.orchard.tv.ui.theme.Glyph
import app.orchard.tv.ui.theme.GlyphIcon
import app.orchard.tv.ui.theme.Palette
import app.orchard.tv.ui.theme.Type
import app.orchard.tv.wallpaper.PhoneServer
import kotlinx.coroutines.*
import org.json.JSONObject
import java.io.File
import kotlin.math.roundToInt

/** One monitor shared by the overview and its child pages; no polling while Control Center is shut. */
@Composable
fun rememberCareSnapshot(visible: Boolean): JSONObject {
    val context = LocalContext.current
    val monitor = remember { SystemMonitor(context) }
    var info by remember { mutableStateOf(JSONObject()) }
    LaunchedEffect(visible) {
        if (!visible) return@LaunchedEffect
        while (true) {
            info = withContext(Dispatchers.IO) { monitor.snapshot() }
            delay(2000)
        }
    }
    return info
}

/** A single focus stop in Control Center; the meters carry the status before opening the page. */
@Composable
fun CareOverview(info: JSONObject, requester: FocusRequester, onClick: () -> Unit) {
    Pressable(onClick, focusRequester = requester, focusedScale = 1.03f, modifier = Modifier.fillMaxWidth()) { state ->
        val fg = if (state.focused) Palette.OnFocus else Palette.Label
        Column(Modifier.fillMaxWidth().background(if (state.focused) Palette.Focus else Palette.Fill, RoundedCornerShape(14.dp)).padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                GlyphIcon(Glyph.Shield, fg, 18.dp)
                BasicText(stringResource(R.string.care_title), style = Type.Body.copy(color = fg), modifier = Modifier.weight(1f).padding(start = 8.dp))
                GlyphIcon(Glyph.ChevronRight, fg.copy(alpha = .6f), 16.dp)
            }
            Row(Modifier.padding(top = 9.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                OverviewMeter(stringResource(R.string.system_memory), fraction(info, "memory"), fg, Modifier.weight(1f))
                OverviewMeter(stringResource(R.string.system_storage), fraction(info, "storage"), fg, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun OverviewMeter(title: String, value: Float?, fg: Color, modifier: Modifier) {
    Column(modifier) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            BasicText(title, style = Type.Caption.copy(color = fg.copy(alpha = .7f)))
            BasicText(percent(value), style = Type.Caption.copy(color = fg))
        }
        Box(Modifier.padding(top = 5.dp).fillMaxWidth().height(3.dp).background(fg.copy(alpha = .12f), RoundedCornerShape(2.dp))) {
            Box(Modifier.fillMaxWidth(value ?: 0f).fillMaxHeight().background(fg.copy(alpha = .8f), RoundedCornerShape(2.dp)))
        }
    }
}

enum class CareFocus { Clean, Details, Exchange }

/** A TV utility, not a diagnostics form: passive meters, one primary action, shallow navigation. */
@Composable
fun CarePage(info: JSONObject, takeFocus: Boolean, initial: CareFocus, onPage: (CcPage, CareFocus) -> Unit,
             onNetwork: () -> Unit, onOpen: (Intent) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val clean = remember { FocusRequester() }
    val details = remember { FocusRequester() }
    val exchange = remember { FocusRequester() }
    var busy by remember { mutableStateOf(false) }
    var result by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(Unit) {
        if (takeFocus) { withFrameNanos { }; runCatching { when (initial) {
            CareFocus.Clean -> clean; CareFocus.Details -> details; CareFocus.Exchange -> exchange
        }.requestFocus() } }
    }
    Column {
        Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            GlyphIcon(Glyph.Shield, Palette.Label, 22.dp)
            BasicText(stringResource(R.string.care_title), style = Type.Title, modifier = Modifier.weight(1f).padding(start = 9.dp))
            Column(horizontalAlignment = Alignment.End) {
                BasicText(stringResource(R.string.care_desktop_cpu), style = Type.Caption)
                BasicText(if (info.has("appCpuPercent")) "${info.optDouble("appCpuPercent").roundToInt()}%" else "—", style = Type.Callout)
            }
        }
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            UsageGauge(stringResource(R.string.system_memory), fraction(info, "memory"), info.optBoolean("memoryLow"), Modifier.weight(1f))
            UsageGauge(stringResource(R.string.system_storage), fraction(info, "storage"), (fraction(info, "storage") ?: 0f) >= .9f, Modifier.weight(1f))
        }
        Spacer(Modifier.height(8.dp))
        Pressable(onNetwork, focusedScale = 1.03f, modifier = Modifier.fillMaxWidth()) { state ->
            val fg = if (state.focused) Palette.OnFocus else Palette.Label
            Row(Modifier.fillMaxWidth().height(48.dp).background(if (state.focused) Palette.Focus else Palette.Fill, RoundedCornerShape(14.dp)).padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                val kind = info.optString("network")
                GlyphIcon(when (kind) { "Wifi" -> Glyph.Wifi; "Ethernet" -> Glyph.Ethernet; else -> Glyph.Offline },
                    if (kind == "Offline") Palette.Orange else fg, 21.dp)
                Column(Modifier.weight(1f)) {
                    BasicText(stringResource(when (kind) { "Wifi" -> R.string.net_wifi; "Ethernet" -> R.string.net_ethernet; else -> R.string.net_offline }), style = Type.Body.copy(color = fg))
                    BasicText("↓ ${rate(info, "rxPerSecond")}   ↑ ${rate(info, "txPerSecond")}", style = Type.Caption.copy(color = fg.copy(alpha = .65f)), maxLines = 1)
                }
                GlyphIcon(Glyph.ChevronRight, fg.copy(alpha = .6f), 16.dp)
            }
        }
        Spacer(Modifier.height(8.dp))
        Pressable(onClick = {
            if (!busy) scope.launch {
                busy = true
                result = null
                val removed = withContext(Dispatchers.IO) { runCatching { SystemMonitor.clean(context) } }
                result = removed.fold({ context.getString(R.string.care_cleaned, SystemMonitor.bytes(context, it)) }, { context.getString(R.string.tools_failed) })
                busy = false
                delay(4000)
                result = null
            }
        }, focusRequester = clean, focusedScale = 1.03f, modifier = Modifier.fillMaxWidth()) { state ->
            val fg = if (state.focused) Palette.OnFocus else Palette.Label
            Row(Modifier.fillMaxWidth().height(44.dp).background(if (state.focused) Palette.Focus else Palette.FillStrong, RoundedCornerShape(14.dp)).padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                GlyphIcon(if (result != null && !busy) Glyph.Check else Glyph.Trash, fg, 19.dp)
                BasicText(result ?: stringResource(if (busy) R.string.care_cleaning else R.string.care_clean), style = Type.Headline.copy(color = fg), maxLines = 1,
                    overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
            }
        }
        Spacer(Modifier.height(6.dp))
        MenuRow(Glyph.Switches, stringResource(R.string.care_background), onClick = {
            onOpen(Intent(Settings.ACTION_MANAGE_APPLICATIONS_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }, trailing = "›")
        MenuRow(Glyph.Info, stringResource(R.string.system_info), onClick = { onPage(CcPage.CareDetails, CareFocus.Details) }, focusRequester = details, trailing = "›")
        MenuRow(Glyph.Usb, stringResource(R.string.transfer_title), onClick = { onPage(CcPage.Exchange, CareFocus.Exchange) }, focusRequester = exchange, trailing = "›")
    }
}

@Composable
private fun UsageGauge(title: String, fraction: Float?, warning: Boolean, modifier: Modifier) {
    val label = "$title ${percent(fraction)}"
    Column(modifier.background(Palette.Fill, RoundedCornerShape(14.dp)).padding(10.dp).semantics(mergeDescendants = true) { contentDescription = label }) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Canvas(Modifier.size(29.dp)) {
                val stroke = Stroke(3.dp.toPx(), cap = StrokeCap.Round)
                drawArc(Palette.Label.copy(alpha = .12f), -90f, 360f, false, style = stroke)
                drawArc(if (warning) Palette.Orange else Palette.Label.copy(alpha = .9f), -90f, 360f * (fraction ?: 0f), false, style = stroke)
            }
            BasicText(percent(fraction), style = Type.Title)
        }
        BasicText(title, style = Type.Callout, modifier = Modifier.padding(top = 8.dp))
    }
}

/** Read-only details have one scroll focus, rather than a fake button for every metric. */
@Composable
fun CareDetails(info: JSONObject, takeFocus: Boolean) {
    val context = LocalContext.current
    val first = remember { FocusRequester() }
    val scroll = rememberScrollState()
    val scope = rememberCoroutineScope()
    LaunchedEffect(Unit) { if (takeFocus) { withFrameNanos { }; runCatching { first.requestFocus() } } }
    Column(Modifier.focusRequester(first).onKeyEvent {
        if (it.type == KeyEventType.KeyDown && (it.key == Key.DirectionDown || it.key == Key.DirectionUp)) {
            scope.launch { scroll.scrollTo((scroll.value + if (it.key == Key.DirectionDown) 100 else -100).coerceIn(0, scroll.maxValue)) }; true
        } else false
    }.focusable().softScrollEdges(scroll).verticalScroll(scroll)) {
        BasicText(stringResource(R.string.system_info), style = Type.Title, modifier = Modifier.padding(4.dp))
        Metric(info.optString("model"), "Android ${info.optString("android")}")
        Metric(stringResource(R.string.system_cpu), info.optString("cpu"))
        Metric(stringResource(R.string.care_desktop_cpu), if (info.has("appCpuPercent")) "${info.optDouble("appCpuPercent").roundToInt()}%" else "—")
        Metric(stringResource(R.string.system_memory), "${SystemMonitor.bytes(context, info.optLong("memoryUsed"))} / ${SystemMonitor.bytes(context, info.optLong("memoryTotal"))}",
            stringResource(R.string.system_app_memory, SystemMonitor.bytes(context, info.optLong("appMemory"))))
        Metric(stringResource(R.string.system_storage), "${SystemMonitor.bytes(context, info.optLong("storageUsed"))} / ${SystemMonitor.bytes(context, info.optLong("storageTotal"))}")
        Metric(stringResource(R.string.system_network), info.optString("addresses").ifBlank { stringResource(R.string.system_offline) })
    }
}

/** File exchange remains a child of TV Care, using exactly the same panel and hardware Back. */
@Composable
fun ExchangePage(takeFocus: Boolean) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val store = remember { TransferStore(File(context.filesDir, "exchange")) }
    var files by remember { mutableStateOf(emptyList<TransferStore.Entry>()) }
    var qr by remember { mutableStateOf<ImageBitmap?>(null) }
    var message by remember { mutableStateOf<String?>(null) }
    var selected by remember { mutableStateOf<TransferStore.Entry?>(null) }
    var busy by remember { mutableStateOf(false) }
    val failed = stringResource(R.string.tools_failed)
    val saved = stringResource(R.string.tools_saved)
    val first = remember { FocusRequester() }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) scope.launch {
            busy = true
            message = withContext(Dispatchers.IO) { runCatching {
                val name = context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { if (it.moveToFirst()) it.getString(0) else "file" } ?: "file"
                context.contentResolver.openInputStream(uri)!!.use { store.add(it, name) }; saved
            }.getOrDefault(failed) }
            files = withContext(Dispatchers.IO) { store.entries() }; busy = false
        }
    }
    val save = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/octet-stream")) { uri ->
        val entry = selected
        if (uri != null && entry != null) scope.launch {
            busy = true
            message = withContext(Dispatchers.IO) { runCatching {
                val file = store.data(entry.id) ?: error("missing")
                context.contentResolver.openOutputStream(uri)!!.use { out -> file.inputStream().use { it.copyTo(out) } }; saved
            }.getOrDefault(failed) }
            busy = false
        }
    }
    DisposableEffect(Unit) { PhoneServer.visible("system", true); onDispose { PhoneServer.visible("system", false) } }
    LaunchedEffect(Unit) {
        if (takeFocus) { withFrameNanos { }; runCatching { first.requestFocus() } }
        val url = withContext(Dispatchers.IO) { PhoneServer.start(context) }
        qr = url?.let { withContext(Dispatchers.Default) { PhoneServer.qr(it).asImageBitmap() } }
        while (true) { files = withContext(Dispatchers.IO) { store.entries() }; delay(2000) }
    }
    Column(Modifier.let { viewport -> val edgeScroll = rememberScrollState(); viewport.softScrollEdges(edgeScroll).verticalScroll(edgeScroll) }) {
        BasicText(stringResource(R.string.transfer_title), style = Type.Title, modifier = Modifier.padding(4.dp))
        qr?.let { Image(it, stringResource(R.string.transfer_title), Modifier.padding(top = 12.dp).size(116.dp).align(Alignment.CenterHorizontally)) }
            ?: BasicText(stringResource(R.string.transfer_connect), style = Type.Callout, modifier = Modifier.padding(12.dp))
        MenuRow(Glyph.Usb, stringResource(R.string.transfer_pick), onClick = {
            if (!busy) runCatching { picker.launch(arrayOf("*/*")) }.onFailure { message = failed }
        }, focusRequester = first, modifier = Modifier.padding(top = 12.dp))
        if (files.isEmpty()) BasicText(stringResource(R.string.transfer_empty), style = Type.Callout, modifier = Modifier.padding(14.dp))
        files.forEach { entry ->
            BasicText(entry.name + " · " + SystemMonitor.bytes(context, entry.bytes), style = Type.Caption, maxLines = 2, modifier = Modifier.padding(14.dp))
            MenuRow(Glyph.Download, stringResource(R.string.transfer_save), onClick = { if (!busy) { selected = entry; runCatching { save.launch(entry.name) }.onFailure { message = failed } } })
            MenuRow(Glyph.Trash, stringResource(R.string.transfer_remove), destructive = true, onClick = {
                if (!busy) scope.launch { withContext(Dispatchers.IO) { store.delete(entry.id) }; files = withContext(Dispatchers.IO) { store.entries() } }
            })
        }
        message?.let { BasicText(it, style = Type.Caption, modifier = Modifier.padding(14.dp)) }
    }
}

@Composable
private fun rate(info: JSONObject, key: String): String {
    val n = info.optLong(key, -1)
    return if (n < 0) "—" else SystemMonitor.bytes(LocalContext.current, n) + "/s"
}
private fun fraction(info: JSONObject, key: String): Float? = info.optLong(key + "Total").takeIf { it > 0 }?.let {
    (info.optLong(key + "Used").toDouble() / it).coerceIn(0.0, 1.0).toFloat()
}
private fun percent(fraction: Float?): String = fraction?.let { "${(it * 100).roundToInt()}%" } ?: "—"

@Composable
private fun Metric(title: String, value: String, detail: String? = null) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        BasicText(title, style = Type.Caption)
        BasicText(value.ifBlank { "—" }, style = Type.Body)
        detail?.let { BasicText(it, style = Type.Caption) }
    }
    Box(Modifier.fillMaxWidth().height(.5.dp).background(Palette.Hairline))
}
