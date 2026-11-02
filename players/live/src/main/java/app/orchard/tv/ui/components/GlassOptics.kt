package app.orchard.tv.ui.components

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

/** Optical profile shared by the static lens and the video shader (in design pixels). */
object GlassOptics {
    const val RADIUS_DP = 32f
    const val DEPTH_DP = 16f
    const val BEND_DP = 9f

    data class Sample(val x: Float, val y: Float, val distance: Float, val nx: Float, val ny: Float)

    fun sample(x: Float, y: Float, width: Float, height: Float, radius: Float, depth: Float, bend: Float): Sample {
        val px = x - width / 2; val py = y - height / 2
        val qx = abs(px) - width / 2 + radius; val qy = abs(py) - height / 2 + radius
        val ox = max(qx, 0f); val oy = max(qy, 0f)
        val len = sqrt(ox * ox + oy * oy)
        val d = len + min(max(qx, qy), 0f) - radius
        val nx = (if (len > 0.001f) ox / len else if (qx > qy) 1f else 0f) * if (px < 0) -1 else 1
        val ny = (if (len > 0.001f) oy / len else if (qy >= qx) 1f else 0f) * if (py < 0) -1 else 1
        val t = (1 + d / depth.coerceAtLeast(1f)).coerceIn(0f, 1f)
        val displacement = bend * t * t * (3 - 2 * t)
        return Sample(x - nx * displacement, y - ny * displacement, d, nx, ny)
    }
}
