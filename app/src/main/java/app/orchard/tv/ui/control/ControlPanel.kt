package app.orchard.tv.ui.control

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.unit.dp
import app.orchard.tv.ui.components.consumeTaps
import app.orchard.tv.ui.components.dismissOnTap
import app.orchard.tv.ui.components.liquidGlass
import app.orchard.tv.ui.theme.LocalReducedMotion
import app.orchard.tv.ui.theme.Motion

/** The shared tvOS control layer: same anchor, material, spacing and focus-sized content. */
@Composable
fun ControlPanel(visible: Boolean, onClose: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    val reduced = LocalReducedMotion.current
    AnimatedVisibility(visible, enter = fadeIn(tween(180)), exit = fadeOut(tween(140)), modifier = Modifier.fillMaxSize()) {
        Box(Modifier.fillMaxSize().dismissOnTap(onClose).background(Color.Black.copy(alpha = .24f)))
    }
    Box(Modifier.fillMaxSize().padding(top = 24.dp, end = 32.dp, bottom = 24.dp), contentAlignment = Alignment.TopEnd) {
        AnimatedVisibility(visible,
            enter = fadeIn(tween(200, easing = Motion.EaseOut)) + scaleIn(tween(if (reduced) 0 else 280, easing = Motion.Drawer), initialScale = .94f, transformOrigin = TransformOrigin(1f, 0f)),
            exit = fadeOut(tween(150)) + scaleOut(tween(if (reduced) 0 else 150), targetScale = .98f, transformOrigin = TransformOrigin(1f, 0f))) {
            Column(Modifier.width(300.dp).heightIn(max = 492.dp).consumeTaps().liquidGlass(24.dp).padding(16.dp), content = content)
        }
    }
}
