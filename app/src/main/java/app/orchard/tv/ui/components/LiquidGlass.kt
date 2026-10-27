package app.orchard.tv.ui.components

import app.orchard.tv.ui.home.LocalMenuScene
import app.orchard.tv.ui.home.LocalMenuSurfaceLight
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
 * Lite uses a smaller baked texture with the same geometry. Layout/background changes rebuild the
 * lens off the main thread; animation frames reuse it. No second decoder or frame readback.
 * [bottomCorners] is false for a sheet that meets the bottom of the screen.
 */
fun Modifier.liquidGlass(corner: Dp, bottomCorners: Boolean = true): Modifier = composed {
    val shape = if (bottomCorners) RoundedCornerShape(corner) else RoundedCornerShape(topStart = corner, topEnd = corner)
    val lite = AerialSettings.liteMode
    val clear = AerialSettings.menuGlassStyle == "clear"
    val frost = LocalMenuScene.current ?: Wallpapers.frosted.takeIf { Wallpapers.backdrop != null }
    val source = LocalMenuScene.current ?: Wallpapers.backdrop
    val lensSource = if (clear) source else frost
    val brightness = AerialSettings.menuGlassLight * LocalMenuSurfaceLight.current
    val density = LocalDensity.current.density
    var at by remember { mutableStateOf(Offset.Zero) }
    var root by remember { mutableStateOf(IntSize.Zero) }
    var extent by remember { mutableStateOf(IntSize.Zero) }
    // Rebuild only after layout/background/settings change; never capture or blur video each frame.
    val lens by produceState<ImageBitmap?>(null, lensSource, root, at, extent, brightness, clear, lite, corner, bottomCorners, density) {
        value = null
        // Coalesce a sheet's entering layout into one bake; its fill and edge answer immediately.
        if (lite) kotlinx.coroutines.delay(80)
        value = if ((!clear && !lite) || lensSource == null || root == IntSize.Zero || extent == IntSize.Zero) null else
            withContext(Dispatchers.Default) {
                val screen = Size(root.width.toFloat(), root.height.toFloat())
                val plate = Rect(at, Size(extent.width.toFloat(), extent.height.toFloat()))
                if (clear) refractivePlate(lensSource, screen, plate, corner.value * density,
                    1.2f * density, brightness, density, if (lite) 768 else 1600, bottomCorners)
                else diffusePlate(lensSource, screen, plate, corner.value * density,
                    1.2f * density, brightness, bottomCorners = bottomCorners)
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
            val scale = if (lite) minOf(1f, 768f / maxOf(w, h)) else 1f
            val tw = (w * scale).roundToInt().coerceAtLeast(1)
            val th = (h * scale).roundToInt().coerceAtLeast(1)
            val mask = smoothMask(tw, th, radius * scale, if (bottomCorners) radius * scale else 0f)
            // Hold the edge beside the draw cache. Global LRU eviction must not cause a bake
            // on each animation frame while this panel is still visible.
            val edge = glassEdge(tw, th, radius * scale, 1.2.dp.toPx() * scale, bottomCorners)
            val brush = shader?.let { ShaderBrush(it) }
            val flat = mask.asImageBitmap()
            onDrawBehind {
                if ((clear || lite) && lens != null) {
                    drawImage(lens!!, dstSize = IntSize(w, h))
                } else if (lite) {
                    drawImage(flat, dstSize = IntSize(w, h), colorFilter = ColorFilter.tint(
                        Color.White.copy(alpha = .08f)))
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
                    drawRect(brush, colorFilter = ColorFilter.colorMatrix(ColorMatrix(floatArrayOf(
                        brightness, 0f, 0f, 0f, 0f, 0f, brightness, 0f, 0f, 0f,
                        0f, 0f, brightness, 0f, 0f, 0f, 0f, 0f, 1f, 0f))), blendMode = BlendMode.SrcIn)
                    drawContext.canvas.restore()
                }
                if (lens == null || (!clear && !lite)) drawImage(edge, dstSize = IntSize(w, h))
            }
        }
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
