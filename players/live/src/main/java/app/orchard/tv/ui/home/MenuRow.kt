package app.orchard.tv.ui.home
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
@Composable
fun MenuRow(
    glyph: Glyph,
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    focusRequester: FocusRequester? = null,
    destructive: Boolean = false,
    trailing: String? = null,
    selected: Boolean = false,
) {
    Pressable(onClick = onClick, focusRequester = focusRequester, focusedScale = 1.03f, modifier = modifier.fillMaxWidth()) { state ->
        val bg by animateColorAsState(
            if (state.focused) Palette.Focus else if (selected) Color.White.copy(alpha = .1f) else Color.Transparent, tween(140, easing = Motion.EaseOut), label = "row-bg",
        )
        val fg = when {
            state.focused -> if (destructive) Color(0xFFD70015) else Palette.OnFocus
            destructive -> Palette.Red
            else -> Palette.Label
        }
        Row(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(bg)
                .height(44.dp)
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            GlyphIcon(glyph, fg, 18.dp)
            BasicText(text, style = Type.Body.copy(color = fg), maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
            if (trailing != null) {
                BasicText(
                    trailing,
                    style = Type.Callout.copy(color = if (state.focused) Palette.OnFocus.copy(alpha = 0.6f) else Palette.SecondaryLabel),
                    maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.widthIn(max = 200.dp),
                )
            }
        }
    }
}

