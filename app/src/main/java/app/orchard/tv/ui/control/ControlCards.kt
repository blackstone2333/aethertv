package app.orchard.tv.ui.control

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import app.orchard.tv.ui.components.softEdges
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.orchard.tv.R
import app.orchard.tv.CcPage
import app.orchard.tv.HomeViewModel
import app.orchard.tv.Overlay
import app.orchard.tv.settings.*
import app.orchard.tv.system.*
import app.orchard.tv.ui.components.Pressable
import app.orchard.tv.ui.components.liquidGlass
import app.orchard.tv.ui.components.softScrollEdges
import app.orchard.tv.ui.home.MenuRow
import app.orchard.tv.ui.home.dateText
import app.orchard.tv.ui.home.timeText
import app.orchard.tv.ui.theme.*
import app.orchard.tv.update.Updates
import org.json.JSONObject
import java.time.LocalDateTime

/** A single optical surface. Its children use ordinary fills rather than nested glass. */
@Composable
internal fun ControlSurface(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier.fillMaxWidth().liquidGlass(22.dp).padding(14.dp), content = content)
}

internal fun ControlKind.title() = when (this) {
    ControlKind.Inputs -> R.string.cc_inputs
    ControlKind.Network -> R.string.cc_network
    ControlKind.Care -> R.string.care_title
    ControlKind.Volume -> R.string.cc_volume
    ControlKind.Brightness -> R.string.cc_brightness
    ControlKind.Settings -> R.string.cc_settings
    ControlKind.Personalize -> R.string.cc_personalize
    ControlKind.Live -> R.string.live_watch
}

private fun ControlKind.glyph() = when (this) {
    ControlKind.Inputs -> Glyph.Input
    ControlKind.Network -> Glyph.Wifi
    ControlKind.Care -> Glyph.Shield
    ControlKind.Volume -> Glyph.Speaker
    ControlKind.Brightness -> Glyph.Sun
    ControlKind.Settings -> Glyph.Gear
    ControlKind.Personalize -> Glyph.Photo
    ControlKind.Live -> Glyph.Antenna
}

@Composable
internal fun MainPage(vm: HomeViewModel, now: LocalDateTime, takeFocus: Boolean, care: JSONObject,
                      initial: String, onSelect: (ControlKind) -> Unit, onEdit: () -> Unit) {
    val specs = AerialSettings.controlLayout
    val placements = remember(specs) { ControlLayout.place(specs) }
    val requesters = remember { ControlKind.entries.associateWith { FocusRequester() } }
    val edit = remember { FocusRequester() }
    val scroll = rememberScrollState()
    LaunchedEffect(Unit) {
        if (takeFocus) {
            withFrameNanos { }
            val target = specs.firstOrNull { it.kind.key == initial && it.visible } ?: specs.firstOrNull { it.visible }
            runCatching { (if (initial == "edit" || target == null) edit else requesters.getValue(target.kind)).requestFocus() }
        }
    }
    Column {
        Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                BasicText(timeText(now), style = Type.LargeTitle)
                BasicText(dateText(now), style = Type.Caption)
            }
            Pressable(onEdit, focusRequester = edit, focusedScale = 1.06f) { state ->
                Row(Modifier.liquidGlass(18.dp).background(if (state.focused) Palette.Focus.copy(alpha = .16f) else Color.Transparent)
                    .padding(horizontal = 13.dp, vertical = 10.dp), horizontalArrangement = Arrangement.spacedBy(7.dp), verticalAlignment = Alignment.CenterVertically) {
                    GlyphIcon(Glyph.Pencil, Palette.Label, 16.dp)
                    BasicText(stringResource(R.string.controls_edit), style = Type.Callout)
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        Column(Modifier.weight(1f, fill = false).softScrollEdges(scroll).verticalScroll(scroll).padding(6.dp)) {
            val rows = placements.maxOfOrNull { it.row + it.spec.size.rows } ?: 0
            Box(Modifier.fillMaxWidth().height((rows * 104 - if (rows > 0) 12 else 0).dp)) {
                placements.forEach { item ->
                    key(item.spec.kind) {
                        val width = (item.spec.size.columns * 105 - 12).dp
                        val height = (item.spec.size.rows * 104 - 12).dp
                        val position = Modifier.offset(x = (item.column * 105).dp, y = (item.row * 104).dp).size(width, height)
                        val kind = item.spec.kind
                        if (kind == ControlKind.Volume || kind == ControlKind.Brightness) {
                            ControlSurface(position) {
                                BasicText(stringResource(kind.title()), style = Type.Callout)
                                Spacer(Modifier.weight(1f))
                                // The slider is the card's single focus target.
                                if (kind == ControlKind.Volume) VolumeRow(vm, requesters.getValue(kind)) else BrightnessRow(vm, requesters.getValue(kind))
                                Spacer(Modifier.weight(1f))
                            }
                        } else {
                            Pressable({ onSelect(kind) }, modifier = position, focusRequester = requesters.getValue(kind), focusedScale = 1.025f) { state ->
                                val shape = RoundedCornerShape(22.dp)
                                Column(Modifier.fillMaxSize().liquidGlass(22.dp)
                                    .border(if (state.focused) 2.dp else 0.dp, if (state.focused) Palette.Focus else Color.Transparent, shape)
                                    .background(if (state.focused) Color.White.copy(alpha = .09f) else Color.Transparent)
                                    .padding(15.dp), verticalArrangement = Arrangement.SpaceBetween) {
                                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                        GlyphIcon(if (kind == ControlKind.Network) networkGlyph(vm.network.kind) else kind.glyph(), Palette.Label, 22.dp)
                                        Spacer(Modifier.weight(1f))
                                        GlyphIcon(Glyph.ChevronRight, Palette.SecondaryLabel, 14.dp)
                                    }
                                    Column {
                                        BasicText(stringResource(kind.title()), style = Type.Headline, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        if (kind == ControlKind.Network) BasicText(networkLabel(vm.network), style = Type.Caption, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        if (kind == ControlKind.Care) {
                                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                                                BasicText(stringResource(R.string.system_memory) + " ${usage(care, "memory")}", style = Type.Caption)
                                                BasicText(stringResource(R.string.system_storage) + " ${usage(care, "storage")}", style = Type.Caption)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun usage(info: JSONObject, key: String): String {
    val total = info.optLong(key + "Total")
    return if (total <= 0) "—" else "${(info.optLong(key + "Used") * 100 / total).coerceIn(0, 100)}%"
}

private fun networkGlyph(kind: NetKind) = when (kind) { NetKind.Wifi -> Glyph.Wifi; NetKind.Ethernet -> Glyph.Ethernet; NetKind.Offline -> Glyph.Offline }
@Composable
private fun networkLabel(net: NetworkStatus): String = when (net.kind) {
    NetKind.Wifi -> net.name ?: stringResource(R.string.net_wifi)
    NetKind.Ethernet -> stringResource(R.string.net_ethernet)
    NetKind.Offline -> stringResource(R.string.net_offline)
}

@Composable
internal fun ControlEditor(takeFocus: Boolean) {
    val first = remember { FocusRequester() }
    val scroll = rememberLazyListState()
    LaunchedEffect(Unit) { if (takeFocus) { withFrameNanos { }; runCatching { first.requestFocus() } } }
    Column {
        BasicText(stringResource(R.string.controls_edit), style = Type.Title, modifier = Modifier.padding(6.dp))
        ControlSurface {
            MenuRow(Glyph.Switches, stringResource(R.string.controls_reset), focusRequester = first, onClick = { AerialSettings.chooseControlLayout(ControlLayout.defaults()) })
        }
        Spacer(Modifier.height(12.dp))
        LazyColumn(Modifier.weight(1f).softEdges(start = { scroll.canScrollBackward }, end = { scroll.canScrollForward }), state = scroll, contentPadding = PaddingValues(4.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(AerialSettings.controlLayout, key = { it.kind.key }) { spec ->
                    ControlSurface {
                        BasicText(stringResource(spec.kind.title()), style = Type.Headline, modifier = Modifier.padding(6.dp))
                        Row {
                            EditAction(stringResource(if (spec.visible) R.string.controls_visible else R.string.controls_hidden), Glyph.Check, Modifier.weight(1f)) {
                                AerialSettings.chooseControlLayout(AerialSettings.controlLayout.map { if (it.kind == spec.kind) it.copy(visible = !it.visible) else it })
                            }
                            EditAction(stringResource(when (spec.size) { ControlSize.Compact -> R.string.controls_compact; ControlSize.Wide -> R.string.controls_wide; ControlSize.Large -> R.string.controls_large }), Glyph.Switches, Modifier.weight(1f)) {
                                val next = ControlSize.entries[(spec.size.ordinal + 1) % ControlSize.entries.size]
                                AerialSettings.chooseControlLayout(AerialSettings.controlLayout.map { if (it.kind == spec.kind) it.copy(size = next) else it })
                            }
                        }
                        Row {
                            EditAction(stringResource(R.string.controls_earlier), Glyph.ChevronLeft, Modifier.weight(1f)) { AerialSettings.chooseControlLayout(ControlLayout.move(AerialSettings.controlLayout, spec.kind, -1)) }
                            EditAction(stringResource(R.string.controls_later), Glyph.ChevronRight, Modifier.weight(1f)) { AerialSettings.chooseControlLayout(ControlLayout.move(AerialSettings.controlLayout, spec.kind, 1)) }
                        }
                    }
            }
        }
    }
}

@Composable
private fun EditAction(label: String, glyph: Glyph, modifier: Modifier, action: () -> Unit) {
    Pressable(action, modifier = modifier.padding(3.dp), focusedScale = 1.025f) { state ->
        Row(Modifier.fillMaxWidth().background(if (state.focused) Palette.Focus else Palette.Fill, RoundedCornerShape(12.dp)).padding(10.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            val fg = if (state.focused) Palette.OnFocus else Palette.Label
            GlyphIcon(glyph, fg, 15.dp)
            BasicText(label, style = Type.Callout.copy(color = fg), maxLines = 1)
        }
    }
}

@Composable
internal fun NetworkPage(vm: HomeViewModel, info: JSONObject, takeFocus: Boolean, onPage: (CcPage) -> Unit, onOpen: (Intent) -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val first = remember { FocusRequester() }
    LaunchedEffect(Unit) { if (takeFocus) { withFrameNanos { }; runCatching { first.requestFocus() } } }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        ControlSurface {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                GlyphIcon(networkGlyph(vm.network.kind), if (vm.network.kind == NetKind.Offline) Palette.Orange else Palette.Label, 30.dp)
                Column {
                    BasicText(stringResource(R.string.cc_network), style = Type.Title)
                    BasicText(networkLabel(vm.network), style = Type.Callout)
                }
            }
            val addresses = info.optString("addresses")
            if (addresses.isNotBlank()) BasicText(addresses, style = Type.Caption, modifier = Modifier.padding(top = 12.dp))
        }
        ControlSurface {
            MenuRow(Glyph.Wifi, stringResource(R.string.network_connections), focusRequester = first, onClick = {
                vm.whenUnlocked {
                    val intents = SystemProbe.networkSettingsIntents(context, ethernet = vm.network.kind == NetKind.Ethernet)
                    LaunchFallback.arm(intents.drop(1)); onOpen(intents.first())
                }
            }, trailing = "›")
            MenuRow(Glyph.Usb, stringResource(R.string.transfer_title), onClick = {
                onPage(CcPage.Exchange)
            }, trailing = "›")
        }
    }
}

@Composable
internal fun SettingsPage(vm: HomeViewModel, takeFocus: Boolean, initial: CcPage?, onPage: (CcPage) -> Unit, onOpen: (Intent) -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val first = remember { FocusRequester() }
    val requesters = remember { CcPage.entries.associateWith { FocusRequester() } }
    val scroll = rememberScrollState()
    LaunchedEffect(Unit) { if (takeFocus) { withFrameNanos { }; runCatching { (requesters[initial] ?: first).requestFocus() } } }
    Column(Modifier.softScrollEdges(scroll).verticalScroll(scroll).padding(4.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        BasicText(stringResource(R.string.cc_settings), style = Type.Title, modifier = Modifier.padding(4.dp))
        ControlSurface {
            MenuRow(Glyph.Photo, stringResource(R.string.cc_personalize), focusRequester = first, onClick = { vm.overlay = Overlay.Wallpaper }, trailing = "›")
            MenuRow(Glyph.Pencil, stringResource(R.string.controls_edit), focusRequester = requesters.getValue(CcPage.EditControls), onClick = { onPage(CcPage.EditControls) }, trailing = "›")
            MenuRow(Glyph.Input, stringResource(R.string.cc_inputs_settings), focusRequester = requesters.getValue(CcPage.InputSettings), onClick = { onPage(CcPage.InputSettings) }, trailing = "›")
        }
        ControlSurface {
            MenuRow(Glyph.Shield, stringResource(R.string.care_title), focusRequester = requesters.getValue(CcPage.Care), onClick = { onPage(CcPage.Care) }, trailing = "›")
            MenuRow(Glyph.Wifi, stringResource(R.string.cc_network), focusRequester = requesters.getValue(CcPage.Network), onClick = { onPage(CcPage.Network) }, trailing = "›")
            MenuRow(Glyph.Gear, stringResource(R.string.system_settings_open), onClick = { vm.whenUnlocked { onOpen(SystemProbe.settingsIntent(context)) } }, trailing = "›")
        }
        ControlSurface {
            MenuRow(Glyph.Info, stringResource(R.string.cc_update_title, Updates.current), focusRequester = requesters.getValue(CcPage.Update), onClick = { onPage(CcPage.Update) }, trailing = "›")
            if (vm.homeStatus == HomeStatus.NotDefault) MenuRow(Glyph.Home, stringResource(R.string.cc_setup_title), focusRequester = requesters.getValue(CcPage.Setup), onClick = { onPage(CcPage.Setup) }, trailing = "›")
        }
    }
}
