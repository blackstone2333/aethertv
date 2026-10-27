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
fun refractivePlate(source: ImageBitmap, screen: Size, plate: Rect, radius: Float, rim: Float, light: Float = .94f, density: Float = radius / GlassOptics.RADIUS_DP): ImageBitmap {
    val scale = min(1f, 1600f / plate.width)
    val w = (plate.width * scale).roundToInt().coerceAtLeast(1)
    val h = (plate.height * scale).roundToInt().coerceAtLeast(1)
    val src = source.asAndroidBitmap()
    val pixels = IntArray(src.width * src.height)
    src.getPixels(pixels, 0, src.width, 0, 0, src.width, src.height)
    val out = IntArray(w * h)
    val r = radius.coerceAtMost(min(plate.width, plate.height) / 2)
    fun channel(x: Float, y: Float, shift: Int): Float {
        val sx = x.coerceIn(0f, src.width - 1f); val sy = y.coerceIn(0f, src.height - 1f)
        val ix = sx.toInt(); val iy = sy.toInt(); val fx = sx - ix; val fy = sy - iy
        fun p(dx: Int, dy: Int) = (pixels[min(iy + dy, src.height - 1) * src.width + min(ix + dx, src.width - 1)] ushr shift) and 255
        return (p(0, 0) * (1 - fx) + p(1, 0) * fx) * (1 - fy) + (p(0, 1) * (1 - fx) + p(1, 1) * fx) * fy
    }
    for (y in 0 until h) for (x in 0 until w) {
        val lens = GlassOptics.sample((x + .5f) / scale, (y + .5f) / scale, plate.width, plate.height,
            r, GlassOptics.DEPTH_DP * density, GlassOptics.BEND_DP * density)
        val sx = (plate.left + lens.x) / screen.width * src.width - .5f
        val sy = (plate.top + lens.y) / screen.height * src.height - .5f
        fun c(shift: Int) = (channel(sx, sy, shift) * light).roundToInt().coerceIn(0, 255)
        out[y * w + x] = (255 shl 24) or (c(16) shl 16) or (c(8) shl 8) or c(0)
    }
    val bitmap = Bitmap.createBitmap(out, w, h, Bitmap.Config.ARGB_8888).copy(Bitmap.Config.ARGB_8888, true)
    val canvas = Canvas(bitmap)
    canvas.drawBitmap(smoothMask(w, h, r * scale), 0f, 0f, Paint().apply {
        xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_IN)
    })
    canvas.drawBitmap(glassEdge(w, h, r * scale, rim * scale).asAndroidBitmap(), 0f, 0f, null)
    return bitmap.frozen().asImageBitmap()
}
