package app.orchard.tv.ui.components

import android.graphics.BitmapShader
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Shader
import androidx.compose.runtime.produceState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.foundation.background
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Paint as ComposePaint
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.core.graphics.createBitmap
import app.orchard.tv.ui.theme.Palette
import app.orchard.tv.settings.AerialSettings
import app.orchard.tv.wallpaper.Wallpapers
import kotlin.math.roundToInt

/**
 * Shared control material: a refractive lens or cached diffusion sampled from the wallpaper.
 * Android 5/6 use the same geometry with the Lite fill. Layout/background changes rebuild the
 * lens off the main thread; animation frames reuse it. No second decoder or frame readback.
 * [bottomCorners] is false for a sheet that meets the bottom of the screen.
 */
fun Modifier.liquidGlass(corner: Dp, bottomCorners: Boolean = true): Modifier = composed {
    val shape = if (bottomCorners) RoundedCornerShape(corner) else RoundedCornerShape(topStart = corner, topEnd = corner)
    if (AerialSettings.liteMode) {
        // Android 5/6 and low-RAM phones benefit more from a flat plate than from a baked mask.
        return@composed this.clip(shape).background(Palette.Glass)
    }
    val clear = AerialSettings.menuGlassStyle == "clear"
    val frost = Wallpapers.frosted.takeIf { Wallpapers.backdrop != null }
    val source = Wallpapers.backdrop
    val brightness = AerialSettings.menuGlassLight
    val density = LocalDensity.current.density
    var at by remember { mutableStateOf(Offset.Zero) }
    var root by remember { mutableStateOf(IntSize.Zero) }
    var extent by remember { mutableStateOf(IntSize.Zero) }
    // Rebuild only after layout/background/settings change; never capture or blur video each frame.
    val lens by produceState<ImageBitmap?>(null, source, root, at, extent, brightness, clear) {
        value = if (!clear || source == null || root == IntSize.Zero || extent == IntSize.Zero) null else
            withContext(Dispatchers.Default) {
                refractivePlate(source, Size(root.width.toFloat(), root.height.toFloat()),
                    Rect(at, Size(extent.width.toFloat(), extent.height.toFloat())), corner.value * density,
                    1.2f * density, brightness, density)
            }
    }
    this
        .onGloballyPositioned { c ->
            at = c.positionInRoot()
            extent = c.size
            var r = c
            while (true) r = r.parentLayoutCoordinates ?: break
            root = r.size
        }
        .clip(shape)
        .drawWithCache {
            val shader = frost?.let { BitmapShader(it.asAndroidBitmap(), Shader.TileMode.CLAMP, Shader.TileMode.CLAMP) }
            val radius = corner.toPx()
            val w = size.width.roundToInt().coerceAtLeast(1)
            val h = size.height.roundToInt().coerceAtLeast(1)
            val mask = smoothMask(w, h, radius, if (bottomCorners) radius else 0f)
            val brush = shader?.let { ShaderBrush(it) }
            val flat = mask.asImageBitmap()
            onDrawBehind {
                if (clear && lens != null) {
                    drawImage(lens!!, dstSize = IntSize(w, h))
                } else if (shader == null || brush == null || root == IntSize.Zero) {
                    drawImage(flat, dstSize = IntSize(w, h), colorFilter = ColorFilter.tint(Palette.Glass))
                } else {
                    shader.setLocalMatrix(Matrix().apply {
                        setScale(root.width.toFloat() / frost.width, root.height.toFloat() / frost.height)
                        postTranslate(-at.x, -at.y)
                    })
                    // The outline is drawn first in a layer of the panel's size and the glass kept
                    // where it lies. One shader per draw: a ComposeShader of the glass and the mask
                    // (1.27-1.30) came out an opaque dark slab on Xiaomi MiTV-ASTP0 (Android 9,
                    // Mali-G31), and before Android 9 hardware drawing cannot compose two bitmaps.
                    drawContext.canvas.saveLayer(Rect(Offset.Zero, size), ComposePaint())
                    drawImage(flat, dstSize = IntSize(w, h))
                    drawRect(brush, colorFilter = PanelTint, blendMode = BlendMode.SrcIn)
                    drawContext.canvas.restore()
                }
                if (!clear || lens == null) drawGlassEdge(size, radius, bottomCorners)
            }
        }
}

/**
 * The lit edge of a piece of glass: a sheen inside the top, and a rim bright where light falls on
 * it along the top and bottom, with neutral grey side walls.
 */
private fun DrawScope.drawGlassEdge(size: Size, radius: Float, bottomCorners: Boolean) {
    val w = size.width.roundToInt()
    val h = size.height.roundToInt()
    if (w <= 0 || h <= 0) return
    drawImage(glassEdge(w, h, radius, 1.2.dp.toPx(), bottomCorners), dstSize = IntSize(w, h))
}

/** The lit edge of a [w]×[h] piece of glass at full strength, white with alpha; made once per size. */
fun glassEdge(w: Int, h: Int, radius: Float, rim: Float, bottomCorners: Boolean = true): ImageBitmap =
    baked(listOf("glass-edge", w, h, radius, rim, bottomCorners)) {
        val bitmap = createBitmap(w, h)
        val canvas = android.graphics.Canvas(bitmap)
        // A sheet's rim runs off the bottom with its corners.
        val bottom = if (bottomCorners) h.toFloat() else h + radius
        fun white(alpha: Float) = Color.White.copy(alpha = alpha).toArgb()
        // The sheen fades out by the middle.
        canvas.drawPath(
            smoothRectPath(Rect(0f, 0f, w.toFloat(), bottom), radius).asAndroidPath(),
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                shader = LinearGradient(0f, 0f, 0f, bottom, intArrayOf(white(0.025f), 0), floatArrayOf(0f, 0.5f), Shader.TileMode.CLAMP)
            },
        )
        // A narrow dark meniscus anchors the highlight; a uniform white outline looks painted on.
        canvas.drawPath(
            smoothRectPath(Rect(rim * 1.8f, rim * 1.8f, w - rim * 1.8f, bottom - rim * 1.8f), radius - rim * 1.8f).asAndroidPath(),
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE; strokeWidth = rim * 1.4f
                color = android.graphics.Color.argb(48, 0, 0, 0)
            },
        )
        canvas.drawPath(
            smoothRectPath(Rect(rim / 2, rim / 2, w - rim / 2, bottom - rim / 2), radius - rim / 2).asAndroidPath(),
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                strokeWidth = rim
                shader = LinearGradient(
                    0f, 0f, 0f, bottom,
                    intArrayOf(white(0.58f), Color(0x807C818A).toArgb(), Color(0x807C818A).toArgb(), white(0.44f)),
                    floatArrayOf(0f, 0.18f, 0.82f, 1f),
                    Shader.TileMode.CLAMP,
                )
            },
        )
        bitmap.frozen().asImageBitmap()
    }

/** Panels: the frosted picture at half its light, lifted a little, so white text reads on snow. */
private val PanelTint = ColorFilter.colorMatrix(ColorMatrix(floatArrayOf(
    0.42f, 0f, 0f, 0f, 8f,
    0f, 0.42f, 0f, 0f, 8f,
    0f, 0f, 0.42f, 0f, 8f,
    0f, 0f, 0f, 1f, 0f,
)))
