package app.orchard.tv.wallpaper

import org.junit.Assert.assertEquals
import org.junit.Test

class PhotoSamplingTest {
    @Test fun narrowTallUploadsAreSampledBeforeDecode() {
        assertEquals(4, Images.photoSampleSize(128, 20_000))
        assertEquals(4, Images.photoSampleSize(20_000, 128))
        assertEquals(16, Images.photoSampleSize(256, 40_960))
    }

    @Test fun ordinaryPhotosKeepTheExistingQualityThreshold() {
        assertEquals(1, Images.photoSampleSize(1920, 1080))
        assertEquals(1, Images.photoSampleSize(4096, 2160))
        assertEquals(1, Images.photoSampleSize(2560, 5119))
        assertEquals(2, Images.photoSampleSize(2560, 5120))
        assertEquals(2, Images.photoSampleSize(7680, 4320))
    }

    @Test fun extremeBoundsCannotOverflowTheSamplingLoop() {
        assertEquals(524_288, Images.photoSampleSize(1, Int.MAX_VALUE))
        assertEquals(524_288, Images.photoSampleSize(Int.MAX_VALUE, 1))
    }
}
