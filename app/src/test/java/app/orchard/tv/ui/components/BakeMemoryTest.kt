package app.orchard.tv.ui.components

import android.graphics.Bitmap
import androidx.compose.ui.graphics.asImageBitmap
import org.junit.After
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [23], manifest = Config.NONE)
class BakeMemoryTest {
    @After fun clear() = clearBakes()

    private fun image(id: String) = baked(listOf(id)) {
        Bitmap.createBitmap(1024, 1024, Bitmap.Config.ARGB_8888)
    }

    @Test fun byteBudgetEvictsTheLeastRecentlyUsedImageWithoutRecyclingVisibleArt() {
        val first = image("first")
        val second = image("second")
        assertSame(first, image("first"))
        image("third")
        assertSame(first, image("first"))
        assertNotSame(second, image("second"))
        assertFalse(second.isRecycled)
        clearBakes()
        assertFalse(first.isRecycled)
    }

    @Test fun aSingleOversizedImageDoesNotRemainInTheGlobalCache() {
        fun large() = baked(listOf("large")) { Bitmap.createBitmap(2048, 2048, Bitmap.Config.ARGB_8888) }
        val visible = large()
        assertNotSame(visible, large())
        assertFalse(visible.isRecycled)
    }

    @Test fun geometryWrappersCountTheirImageBytesToo() {
        class Wrapped(override val image: androidx.compose.ui.graphics.ImageBitmap) : BakedImage
        fun wrapped(id: String) = baked(listOf(id)) { Wrapped(Bitmap.createBitmap(1024, 1024, Bitmap.Config.ARGB_8888).asImageBitmap()) }
        val old = wrapped("a")
        wrapped("b"); wrapped("c")
        assertNotSame(old, wrapped("a"))
        assertEquals(1024, old.image.width)
    }
}
