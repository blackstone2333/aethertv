package app.orchard.tv.ui.components

import android.graphics.Bitmap
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.asImageBitmap
import org.junit.After
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [23], manifest = Config.NONE)
class GlassTextureBudgetTest {
    @After fun clear() = clearBakes()

    @Test fun portraitAndLandscapeLensesHonorBothTextureDimensionsAndKeepTheirSourceAlive() {
        val bitmap = Bitmap.createBitmap(256, 144, Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(android.graphics.Color.BLUE)
        val source = bitmap.asImageBitmap()
        for (plate in listOf(Rect(0f, 0f, 1600f, 400f), Rect(0f, 0f, 400f, 1600f))) {
            val clear = refractivePlate(source, Size(1920f, 1920f), plate, 32f, 1.2f, maxTextureSize = 64)
            val diffuse = diffusePlate(source, Size(1920f, 1920f), plate, 32f, 1.2f, .5f, maxTextureSize = 64)
            for (image in listOf(clear, diffuse)) {
                assertEquals(64, maxOf(image.width, image.height))
                assertEquals(16, minOf(image.width, image.height))
            }
        }
        assertFalse(bitmap.isRecycled)
        assertEquals(android.graphics.Color.BLUE, bitmap.getPixel(0, 0))
    }
}
