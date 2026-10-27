package app.orchard.tv.ui.components

import androidx.compose.foundation.ScrollState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.unit.dp

/** Alpha diffusion at overflowing viewport edges; no opaque strip painted over the content. */
fun Modifier.softEdges(horizontal: Boolean = false, start: () -> Boolean, end: () -> Boolean): Modifier =
    drawWithContent {
        val before = start(); val after = end()
        if (!before && !after) { drawContent(); return@drawWithContent }
        val extent = if (horizontal) size.width else size.height
        if (extent <= 0f) { drawContent(); return@drawWithContent }
        val fade = minOf(24.dp.toPx(), extent / 4)
        drawContext.canvas.saveLayer(Rect(Offset.Zero, size), Paint())
        drawContent()
        val stops = arrayOf(0f to if (before) Color.Transparent else Color.Black,
            fade / extent to Color.Black, 1f - fade / extent to Color.Black,
            1f to if (after) Color.Transparent else Color.Black)
        drawRect(if (horizontal) Brush.horizontalGradient(*stops) else Brush.verticalGradient(*stops), blendMode = BlendMode.DstIn)
        drawContext.canvas.restore()
    }

fun Modifier.softScrollEdges(state: ScrollState, horizontal: Boolean = false): Modifier =
    softEdges(horizontal, start = { state.value > 0 }, end = { state.value < state.maxValue })
