package app.orchard.tv.ui.components

import android.graphics.Bitmap
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import kotlin.math.*

/** A static lens baked only when its source or geometry changes; works before RuntimeShader. */
fun refractivePlate(source: ImageBitmap, screen: Size, plate: Rect, radius: Float, rim: Float): ImageBitmap {
    // Bound CPU work and texture memory on 4K canvases. The GPU only scales this finished image.
    val scale = min(1f, 1400f / plate.width)
    val w = (plate.width * scale).roundToInt().coerceAtLeast(1)
    val h = (plate.height * scale).roundToInt().coerceAtLeast(1)
    val src = source.asAndroidBitmap()
    val pixels = IntArray(src.width * src.height)
    src.getPixels(pixels, 0, src.width, 0, 0, src.width, src.height)
    val out = IntArray(w * h)
    val r = radius.coerceAtMost(min(plate.width, plate.height) / 2)
    for (y in 0 until h) for (x in 0 until w) {
        val px = (x + 0.5f) / scale
        val py = (y + 0.5f) / scale
        val cx = px.coerceIn(r, plate.width - r)
        val cy = py.coerceIn(r, plate.height - r)
        var nx = px - cx; var ny = py - cy
        val length = sqrt(nx * nx + ny * ny)
        val distance: Float
        if (length > 0.001f) { nx /= length; ny /= length; distance = r - length }
        else {
            val d = minOf(px, plate.width - px, py, plate.height - py)
            nx = if (d == px) -1f else if (d == plate.width - px) 1f else 0f
            ny = if (d == py) -1f else if (d == plate.height - py) 1f else 0f
            distance = d
        }
        val band = (1f - distance / (r * 0.8f).coerceAtLeast(1f)).coerceIn(0f, 1f)
        val bend = band * band * r * 0.32f
        val sx = ((plate.left + px - nx * bend) / screen.width * src.width).toInt().coerceIn(0, src.width - 1)
        val sy = ((plate.top + py - ny * bend) / screen.height * src.height).toInt().coerceIn(0, src.height - 1)
        val color = pixels[sy * src.width + sx]
        val light = (8 + band * 12).toInt()
        fun channel(shift: Int) = (((color shr shift) and 255) * 0.98f + light).toInt().coerceIn(0, 255)
        out[y * w + x] = (218 shl 24) or (channel(16) shl 16) or (channel(8) shl 8) or channel(0)
    }
    val bitmap = Bitmap.createBitmap(out, w, h, Bitmap.Config.ARGB_8888).copy(Bitmap.Config.ARGB_8888, true)
    val canvas = android.graphics.Canvas(bitmap)
    canvas.drawBitmap(smoothMask(w, h, r * scale), 0f, 0f, android.graphics.Paint().apply {
        xfermode = android.graphics.PorterDuffXfermode(android.graphics.PorterDuff.Mode.DST_IN)
    })
    canvas.drawBitmap(glassEdge(w, h, r * scale, rim * scale).asAndroidBitmap(), 0f, 0f, null)
    return bitmap.frozen().asImageBitmap()
}
