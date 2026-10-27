package app.orchard.tv.ui.home

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import app.orchard.tv.apps.AppEntry
import app.orchard.tv.settings.AerialSettings
import app.orchard.tv.wallpaper.Images
import app.orchard.tv.wallpaper.Wallpapers
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.time.LocalDateTime

/** One small, softly diffused Home scene shared by all floating controls. */
val LocalMenuScene = staticCompositionLocalOf<ImageBitmap?> { null }

/** Personalization needs more text contrast than the compact Control Center controls. */
val LocalMenuSurfaceLight = staticCompositionLocalOf { 1f }

@Composable
internal fun rememberMenuScene(visible: Boolean, apps: List<AppEntry>, metrics: GridMetrics,
                              width: Float, height: Float, scrollDp: Float, now: LocalDateTime): ImageBitmap? {
    val wallpaper = Wallpapers.backdrop
    val selected = Wallpapers.selectedId
    val plate = AerialSettings.homePlate
    val right = AerialSettings.clockRight
    val large = AerialSettings.largeClock
    val clock = timeText(now)
    val scene by produceState<ImageBitmap?>(null, visible, wallpaper, selected, apps, metrics, plate, right, large) {
        if (!visible) { delay(400); value = null; return@produceState }
        // No hardware layer readback, ImageReader, video copy or secondary decoder, including API21.
        value = withContext(Dispatchers.Default) {
            val w = 480
            val h = (w * height / width).toInt().coerceAtLeast(1)
            val out = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(out)
            val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
            val picture = Wallpapers.resolve(selected)?.let { Wallpapers.picture(it, w, h) }
            val fallback = wallpaper ?: apps.firstOrNull()?.art?.backdrop
            val background = picture ?: fallback?.asAndroidBitmap()
            if (background != null) canvas.drawBitmap(background, null, RectF(0f, 0f, w.toFloat(), h.toFloat()), paint)
            if (picture != null) picture.recycle()
            canvas.save()
            canvas.scale(w / width, h / height)
            if (plate && scrollDp < metrics.tileH.value) {
                paint.color = android.graphics.Color.argb(30, 235, 240, 245)
                canvas.drawRoundRect(RectF(metrics.margin.value - 22f, metrics.topRowY.value - scrollDp - 20f,
                    width - metrics.margin.value + 22f, metrics.topRowY.value - scrollDp + metrics.tileH.value + 20f), 32f, 32f, paint)
            }
            apps.forEachIndexed { i, app ->
                val (dx, dy) = metrics.slot(i, scrollDp < metrics.tileH.value)
                val x = dx.value; val y = dy.value - scrollDp
                if (y + metrics.tileH.value < 0f || y > height) return@forEachIndexed
                val art = app.art
                val target = RectF(x, y, x + metrics.tileW.value, y + metrics.tileH.value)
                paint.color = android.graphics.Color.WHITE
                canvas.drawBitmap(art.back.asAndroidBitmap(), null, target, paint)
                art.fore?.let { fore ->
                    val r = art.foreAt
                    canvas.drawBitmap(fore.asAndroidBitmap(), null, RectF(x + r.left * target.width(), y + r.top * target.height(),
                        x + r.right * target.width(), y + r.bottom * target.height()), paint)
                }
            }
            paint.color = android.graphics.Color.WHITE
            paint.textSize = if (large) 64f else 24f
            canvas.drawText(clock, if (right) width - metrics.margin.value - paint.measureText(clock) else metrics.margin.value,
                if (large) 150f else 56f, paint)
            canvas.restore()
            val pixels = IntArray(w * h)
            out.getPixels(pixels, 0, w, 0, 0, w, h)
            repeat(2) { Images.boxBlur(pixels, w, h, 2) }
            out.setPixels(pixels, 0, w, 0, 0, w, h)
            out.asImageBitmap()
        }
    }
    return scene
}
