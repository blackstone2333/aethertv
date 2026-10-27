package app.orchard.tv.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import app.orchard.tv.ui.components.Pressable
import app.orchard.tv.ui.theme.Glyph
import app.orchard.tv.ui.theme.GlyphIcon
import app.orchard.tv.ui.theme.Palette

/** Direct system controls share the status pill's focus and geometry. */
@Composable
fun HomeControl(glyph: Glyph, label: String, requester: FocusRequester, enabled: Boolean,
                onFocused: (Boolean) -> Unit, onClick: () -> Unit) {
    Pressable(onClick = onClick, enabled = enabled, focusRequester = requester, focusedScale = 1.08f,
        modifier = Modifier.semantics { contentDescription = label }) { state ->
        LaunchedEffect(state.focused) { onFocused(state.focused) }
        Box(Modifier.size(38.dp).clip(CircleShape).background(if (state.focused) Palette.Focus else Color.Transparent),
            contentAlignment = Alignment.Center) {
            GlyphIcon(glyph, if (state.focused) Palette.OnFocus else Color.White.copy(alpha = .9f), 18.dp)
        }
    }
}
