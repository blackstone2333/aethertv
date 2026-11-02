package app.orchard.tv.live.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.orchard.tv.ui.components.Pressable
import app.orchard.tv.ui.theme.*

/** Channel identity, selection and focus remain distinct at remote viewing distance. */
@Composable
fun ChannelRow(number: Int, title: String, group: String?, selected: Boolean, requester: FocusRequester?, onClick: () -> Unit) {
    Pressable(onClick, focusRequester = requester, focusedScale = 1.025f, modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)) { state ->
        val bg by animateColorAsState(if (state.focused) Palette.Focus else if (selected) Color.White.copy(alpha = .1f) else Color.Transparent,
            tween(140, easing = Motion.EaseOut), label = "channel-focus")
        val fg = if (state.focused) Palette.OnFocus else Palette.Label
        val secondary = fg.copy(alpha = if (state.focused) .6f else .65f)
        Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(bg).padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            BasicText(number.toString().padStart(2, '0'), style = Type.Caption.copy(color = secondary), modifier = Modifier.width(24.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                BasicText(title, style = Type.Headline.copy(color = fg), maxLines = 1, overflow = TextOverflow.Ellipsis)
                group?.takeIf { it.isNotBlank() }?.let { BasicText(it, style = Type.Caption.copy(color = secondary), maxLines = 1, overflow = TextOverflow.Ellipsis) }
            }
            if (selected) GlyphIcon(Glyph.Check, fg, 16.dp)
        }
    }
}
