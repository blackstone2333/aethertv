package app.orchard.tv.ui.components

import org.junit.Assert.*
import org.junit.Test

class GlassOpticsTest {
    private fun ray(x: Float, y: Float) = GlassOptics.sample(x, y, 400f, 100f, 32f, 16f, 9f)

    @Test fun contentAtTheCenterIsNotDistortedAndTheLensStaysWithinItsBoundary() {
        val center = ray(200f, 50f)
        assertEquals(200f, center.x, .001f)
        assertEquals(50f, center.y, .001f)
        val top = ray(200f, 1f)
        assertTrue(top.y > 1f && top.y <= 10f)
        val inner = ray(200f, 18f)
        assertEquals(18f, inner.y, .001f)
    }

    @Test fun oppositeEdgesAndCornersHaveSymmetricContinuousRefraction() {
        for (x in 1..399 step 7) for (y in 1..99 step 5) {
            val a = ray(x.toFloat(), y.toFloat())
            val b = ray(400f - x, 100f - y)
            assertEquals(400f, a.x + b.x, .001f)
            assertEquals(100f, a.y + b.y, .001f)
            assertTrue(a.x.isFinite() && a.y.isFinite())
        }
        // Crossing from the straight edge to its rounded corner must not produce a seam.
        val a = ray(31.999f, 1f); val b = ray(32.001f, 1f)
        assertEquals(a.x, b.x, .01f); assertEquals(a.y, b.y, .01f)
    }
}
