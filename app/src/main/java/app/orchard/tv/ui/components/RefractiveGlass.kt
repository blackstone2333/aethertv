package app.orchard.tv.ui.components

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import kotlin.math.*

/** A color-neutral, bilinear lens. Static backgrounds bake once; video uses the same profile on GPU. */
fun refractivePlate(source: ImageBitmap, screen: Size, plate: Rect, radius: Float, rim: Float, light: Float = .94f, density: Float = radius / GlassOptics.RADIUS_DP, maxTextureSize: Int = 1600, bottomCorners: Boolean = true): ImageBitmap {
    val scale = min(1f, maxTextureSize / max(plate.width, plate.height))
    val w = (plate.width * scale).roundToInt().coerceAtLeast(1)
    val h = (plate.height * scale).roundToInt().coerceAtLeast(1)
    // Lite reads a bounded source as well as producing a bounded lens: a tiny control should
    // not allocate another 4K-sized IntArray merely to sample the wallpaper underneath it.
    val original = source.asAndroidBitmap()
    val sampleScale = min(1f, maxTextureSize.toFloat() / max(original.width, original.height))
    val src = if (sampleScale < 1f) Bitmap.createScaledBitmap(original,
        (original.width * sampleScale).roundToInt().coerceAtLeast(1),
        (original.height * sampleScale).roundToInt().coerceAtLeast(1), true) else original
    val pixels = IntArray(src.width * src.height)
    src.getPixels(pixels, 0, src.width, 0, 0, src.width, src.height)
    val srcW = src.width; val srcH = src.height
    if (src !== original) src.recycle()
    val out = IntArray(w * h)
    val r = radius.coerceAtMost(min(plate.width, plate.height) / 2)
    fun channel(x: Float, y: Float, shift: Int): Float {
        val sx = x.coerceIn(0f, srcW - 1f); val sy = y.coerceIn(0f, srcH - 1f)
        val ix = sx.toInt(); val iy = sy.toInt(); val fx = sx - ix; val fy = sy - iy
        fun p(dx: Int, dy: Int) = (pixels[min(iy + dy, srcH - 1) * srcW + min(ix + dx, srcW - 1)] ushr shift) and 255
        return (p(0, 0) * (1 - fx) + p(1, 0) * fx) * (1 - fy) + (p(0, 1) * (1 - fx) + p(1, 1) * fx) * fy
    }
    for (y in 0 until h) for (x in 0 until w) {
        val lens = GlassOptics.sample((x + .5f) / scale, (y + .5f) / scale, plate.width, plate.height,
            r, GlassOptics.DEPTH_DP * density, GlassOptics.BEND_DP * density)
        val sx = (plate.left + lens.x) / screen.width * srcW - .5f
        val sy = (plate.top + lens.y) / screen.height * srcH - .5f
        fun c(shift: Int) = (channel(sx, sy, shift) * light).roundToInt().coerceIn(0, 255)
        out[y * w + x] = (255 shl 24) or (c(16) shl 16) or (c(8) shl 8) or c(0)
    }
    val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    bitmap.setPixels(out, 0, w, 0, 0, w, h)
    val canvas = Canvas(bitmap)
    canvas.drawBitmap(smoothMask(w, h, r * scale, if (bottomCorners) r * scale else 0f), 0f, 0f, Paint().apply {
        xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_IN)
    })
    canvas.drawBitmap(glassEdge(w, h, r * scale, rim * scale, bottomCorners).asAndroidBitmap(), 0f, 0f, null)
    return bitmap.frozen().asImageBitmap()
}

/** Already blurred source, tinted and masked once at the same bounded size as a Lite clear lens. */
fun diffusePlate(source: ImageBitmap, screen: Size, plate: Rect, radius: Float, rim: Float,
    light: Float, maxTextureSize: Int = 768, bottomCorners: Boolean = true): ImageBitmap {
    val scale = min(1f, maxTextureSize / max(plate.width, plate.height))
    val w = (plate.width * scale).roundToInt().coerceAtLeast(1)
    val h = (plate.height * scale).roundToInt().coerceAtLeast(1)
    val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    Canvas(bitmap).apply {
        drawBitmap(source.asAndroidBitmap(), android.graphics.Matrix().apply {
            setScale(screen.width / source.width * scale, screen.height / source.height * scale)
            postTranslate(-plate.left * scale, -plate.top * scale)
        }, Paint(Paint.FILTER_BITMAP_FLAG).apply {
            colorFilter = android.graphics.ColorMatrixColorFilter(floatArrayOf(
                light, 0f, 0f, 0f, 8f, 0f, light, 0f, 0f, 8f,
                0f, 0f, light, 0f, 8f, 0f, 0f, 0f, 1f, 0f))
        })
        drawBitmap(smoothMask(w, h, radius * scale, if (bottomCorners) radius * scale else 0f), 0f, 0f, Paint().apply {
            xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_IN)
        })
        drawBitmap(glassEdge(w, h, radius * scale, rim * scale, bottomCorners).asAndroidBitmap(), 0f, 0f, null)
    }
    return bitmap.frozen().asImageBitmap()
}
