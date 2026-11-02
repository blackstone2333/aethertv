package app.orchard.tv.ui.components

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

/** A single optical region is rendered inside the video's existing OES pass. */
class VideoGlassState {
    var bounds by mutableStateOf<Rect?>(null)
    var radius by mutableFloatStateOf(24f)
    var regular by mutableStateOf(true)
}
val LocalVideoGlass = staticCompositionLocalOf<VideoGlassState?> { null }
private val LocalStaticGlass = staticCompositionLocalOf<ImageBitmap?> { null }

/** Small immutable scene: no screenshots, personal artwork, idle animation or frame readback. */
@Composable
fun GlassSettingsScene(content: @Composable BoxScope.() -> Unit) {
    val scene = remember {
        val bitmap = Bitmap.createBitmap(768, 432, Bitmap.Config.ARGB_8888)
        Canvas(bitmap).apply {
            drawColor(android.graphics.Color.rgb(9, 14, 19))
            val paint = Paint(Paint.ANTI_ALIAS_FLAG)
            paint.shader = RadialGradient(610f, 85f, 510f, intArrayOf(0xFF52636A.toInt(), 0xFF202D36.toInt(), 0xFF090E13.toInt()), floatArrayOf(0f, .45f, 1f), Shader.TileMode.CLAMP)
            drawRect(0f, 0f, 768f, 432f, paint)
            paint.shader = RadialGradient(130f, 420f, 390f, intArrayOf(0x70435A60, 0x00435A60), null, Shader.TileMode.CLAMP)
            drawRect(0f, 0f, 768f, 432f, paint)
        }
        bitmap.asImageBitmap()
    }
    CompositionLocalProvider(LocalStaticGlass provides scene) {
        Box(Modifier.fillMaxSize()) {
            Image(scene, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.FillBounds)
            content()
        }
    }
}

/** Shared media material: live same-frame refraction, or a bounded static lens baked on layout. */
fun Modifier.liquidGlass(corner: Dp, regular: Boolean = true): Modifier = composed {
    val video = LocalVideoGlass.current
    val source = LocalStaticGlass.current
    val density = LocalDensity.current.density
    var bounds by remember { mutableStateOf(Rect.Zero) }
    var root by remember { mutableStateOf(IntSize.Zero) }
    val lens by produceState<ImageBitmap?>(null, source, bounds, root, corner, density) {
        value = if (source == null || bounds.width <= 0 || bounds.height <= 0 || root == IntSize.Zero) null
        else withContext(Dispatchers.Default) { staticLens(source, root, bounds, corner.value * density, density) }
    }
    this.onGloballyPositioned { c ->
        bounds = c.boundsInRoot()
        var parent = c
        while (parent.parentLayoutCoordinates != null) parent = parent.parentLayoutCoordinates!!
        root = parent.size
        video?.let { it.bounds = bounds; it.radius = corner.value; it.regular = regular }
    }.clip(RoundedCornerShape(corner)).drawWithCache {
        val radius = CornerRadius(corner.toPx())
        val rim = .7f * density
        val edge = Brush.verticalGradient(listOf(Color.White.copy(alpha = .38f), Color(0x166E7780), Color(0x126E7780), Color.White.copy(alpha = .22f)))
        val sheen = Brush.verticalGradient(listOf(Color.White.copy(alpha = .035f), Color.Transparent, Color.Transparent))
        onDrawBehind {
            val image = lens
            if (image != null) drawImage(image, dstSize = IntSize(size.width.roundToInt(), size.height.roundToInt()))
            else drawRect(Color.Black.copy(alpha = if (video != null) { if (regular) .14f else .08f } else if (regular) .77f else .42f))
            drawRoundRect(sheen, cornerRadius = radius)
            drawRoundRect(Color.Black.copy(alpha = .22f), topLeft = Offset(rim * 2, rim * 2),
                size = Size((size.width - rim * 4).coerceAtLeast(0f), (size.height - rim * 4).coerceAtLeast(0f)), cornerRadius = radius, style = Stroke(rim * 1.5f))
            drawRoundRect(edge, topLeft = Offset(rim / 2, rim / 2), size = Size((size.width - rim).coerceAtLeast(0f), (size.height - rim).coerceAtLeast(0f)), cornerRadius = radius, style = Stroke(rim))
        }
    }
}

/** Cached, color-neutral lens using the same edge profile as the video shader. */
private fun staticLens(source: ImageBitmap, root: IntSize, plate: Rect, radius: Float, density: Float): ImageBitmap {
    val scale = minOf(1f, 768f / maxOf(plate.width, plate.height))
    val w = (plate.width * scale).roundToInt().coerceAtLeast(1)
    val h = (plate.height * scale).roundToInt().coerceAtLeast(1)
    val bitmap = source.asAndroidBitmap()
    val src = IntArray(bitmap.width * bitmap.height)
    bitmap.getPixels(src, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
    val pixels = IntArray(w * h)
    for (y in 0 until h) for (x in 0 until w) {
        val sample = GlassOptics.sample((x + .5f) / scale, (y + .5f) / scale, plate.width, plate.height,
            radius.coerceAtMost(minOf(plate.width, plate.height) / 2), GlassOptics.DEPTH_DP * density, GlassOptics.BEND_DP * density)
        val sx = ((plate.left + sample.x) / root.width * bitmap.width).toInt().coerceIn(0, bitmap.width - 1)
        val sy = ((plate.top + sample.y) / root.height * bitmap.height).toInt().coerceIn(0, bitmap.height - 1)
        val c = src[sy * bitmap.width + sx]
        fun component(shift: Int) = (((c ushr shift) and 255) * .86f + 3).roundToInt().coerceIn(0, 255)
        pixels[y * w + x] = (255 shl 24) or (component(16) shl 16) or (component(8) shl 8) or component(0)
    }
    return Bitmap.createBitmap(pixels, w, h, Bitmap.Config.ARGB_8888).asImageBitmap()
}
