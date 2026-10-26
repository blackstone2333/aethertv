package app.orchard.tv.ui.control

import android.provider.OpenableColumns
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.orchard.tv.R
import app.orchard.tv.system.SystemMonitor
import app.orchard.tv.system.TransferStore
import app.orchard.tv.ui.components.liquidGlass
import app.orchard.tv.ui.home.MenuRow
import app.orchard.tv.ui.theme.Glyph
import app.orchard.tv.ui.theme.Type
import app.orchard.tv.wallpaper.PhoneServer
import kotlinx.coroutines.*
import org.json.JSONObject
import java.io.File

/** Tools run only while this panel is visible; file access always uses the system picker. */
@Composable
fun SystemTools(onClose: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val monitor = remember { SystemMonitor(context) }
    val store = remember { TransferStore(File(context.filesDir, "exchange")) }
    var info by remember { mutableStateOf(JSONObject()) }
    var files by remember { mutableStateOf(emptyList<TransferStore.Entry>()) }
    var qr by remember { mutableStateOf<ImageBitmap?>(null) }
    var message by remember { mutableStateOf<String?>(null) }
    var selected by remember { mutableStateOf<TransferStore.Entry?>(null) }
    var busy by remember { mutableStateOf(false) }
    val failed = stringResource(R.string.tools_failed)
    val saved = stringResource(R.string.tools_saved)
    val first = remember { FocusRequester() }
    fun bytes(n: Long) = SystemMonitor.bytes(context, n)
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) scope.launch {
            busy = true
            message = withContext(Dispatchers.IO) {
                runCatching {
                    val name = context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use {
                        if (it.moveToFirst()) it.getString(0) else "file"
                    } ?: "file"
                    context.contentResolver.openInputStream(uri)!!.use { store.add(it, name) }
                    saved
                }.getOrDefault(failed)
            }
            busy = false
        }
    }
    val save = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/octet-stream")) { uri ->
        val entry = selected
        if (uri != null && entry != null) scope.launch {
            busy = true
            message = withContext(Dispatchers.IO) {
                runCatching {
                    val file = store.data(entry.id) ?: error("missing")
                    context.contentResolver.openOutputStream(uri)!!.use { out -> file.inputStream().use { it.copyTo(out) } }
                    saved
                }.getOrDefault(failed)
            }
            busy = false
        }
    }
    BackHandler(onBack = onClose)
    DisposableEffect(Unit) {
        PhoneServer.visible("system", true)
        onDispose { PhoneServer.visible("system", false) }
    }
    LaunchedEffect(Unit) {
        val url = withContext(Dispatchers.IO) { PhoneServer.start(context) }
        qr = url?.let { withContext(Dispatchers.Default) { PhoneServer.qr(it).asImageBitmap() } }
        withFrameNanos { }; runCatching { first.requestFocus() }
        while (true) {
            info = withContext(Dispatchers.IO) { monitor.snapshot() }
            files = withContext(Dispatchers.IO) { store.entries() }
            delay(2000)
        }
    }
    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.42f)), contentAlignment = Alignment.Center) {
        Row(Modifier.width(860.dp).height(464.dp).liquidGlass(28.dp).padding(24.dp), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                MenuRow(Glyph.ChevronLeft, stringResource(R.string.system_info), onClick = onClose, focusRequester = first)
                BasicText(info.optString("model") + " · Android " + info.optString("android"), style = Type.Callout)
                InfoCard(stringResource(R.string.system_cpu), "${info.optString("cpu")} · ${info.optInt("cores")} " + stringResource(R.string.system_cores),
                    stringResource(R.string.system_app_cpu, info.optDouble("appCpuPercent", 0.0).toInt()))
                InfoCard(stringResource(R.string.system_memory), "${bytes(info.optLong("memoryUsed"))} / ${bytes(info.optLong("memoryTotal"))}",
                    stringResource(R.string.system_app_memory, bytes(info.optLong("appMemory"))))
                InfoCard(stringResource(R.string.system_storage), "${bytes(info.optLong("storageUsed"))} / ${bytes(info.optLong("storageTotal"))}", stringResource(R.string.system_used))
                val rate = "↓ ${bytes(info.optLong("rxPerSecond"))}/s   ↑ ${bytes(info.optLong("txPerSecond"))}/s"
                InfoCard(stringResource(R.string.system_network), rate, info.optString("addresses").ifBlank { stringResource(R.string.system_offline) })
                MenuRow(Glyph.Trash, stringResource(R.string.system_clean), onClick = {
                    if (!busy) scope.launch {
                        busy = true
                        val removed = withContext(Dispatchers.IO) { SystemMonitor.clean(context) }
                        message = context.getString(R.string.system_cleaned, bytes(removed)); busy = false
                    }
                })
                BasicText(stringResource(R.string.system_clean_hint), style = Type.Caption, maxLines = 3)
                message?.let { BasicText(it, style = Type.Callout) }
            }
            Column(Modifier.width(292.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                BasicText(stringResource(R.string.transfer_title), style = Type.Title)
                qr?.let { Image(it, contentDescription = stringResource(R.string.transfer_title), modifier = Modifier.size(122.dp).align(Alignment.CenterHorizontally)) }
                    ?: BasicText(stringResource(R.string.transfer_connect), style = Type.Callout)
                BasicText(stringResource(R.string.transfer_hint), style = Type.Caption)
                MenuRow(Glyph.Usb, stringResource(R.string.transfer_pick), onClick = {
                    if (!busy) runCatching { picker.launch(arrayOf("*/*")) }.onFailure { message = failed }
                })
                if (files.isEmpty()) BasicText(stringResource(R.string.transfer_empty), style = Type.Callout)
                files.forEach { entry ->
                    BasicText(entry.name + " · " + bytes(entry.bytes), style = Type.Caption, maxLines = 2)
                    MenuRow(Glyph.Download, stringResource(R.string.transfer_save), onClick = {
                        if (!busy) { selected = entry; runCatching { save.launch(entry.name) }.onFailure { message = failed } }
                    })
                    MenuRow(Glyph.Trash, stringResource(R.string.transfer_remove), destructive = true, onClick = {
                        if (!busy) scope.launch { withContext(Dispatchers.IO) { store.delete(entry.id) }; files = withContext(Dispatchers.IO) { store.entries() } }
                    })
                }
            }
        }
    }
}

@Composable
private fun InfoCard(title: String, value: String, detail: String) {
    Column(Modifier.fillMaxWidth().background(Color.White.copy(alpha = 0.06f), RoundedCornerShape(14.dp)).padding(14.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
        BasicText(title, style = Type.Caption)
        BasicText(value, style = Type.Headline)
        BasicText(detail, style = Type.Caption)
    }
}
