package app.orchard.tv.live

import android.content.Intent
import app.orchard.tv.media.MediaContract
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.Shadows.shadowOf
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [23, 28])
class LaunchContractTest {
    @Test fun routesValidSettingsRequestDeliveredToExistingSingleTaskRoot() {
        val controller = Robolectric.buildActivity(LiveActivity::class.java, Intent(Intent.ACTION_MAIN)).create()
        controller.newIntent(Intent(MediaContract.SETTINGS).putExtra(MediaContract.EXTRA_VERSION, 1))
        assertTrue(shadowOf(controller.get()).nextStartedActivity.component?.className == LiveSettingsActivity::class.java.name)
        controller.destroy()
    }

    @Test fun playRejectsMissingAndFutureVersions() {
        for (version in listOf(-1, 0, 2)) {
            val controller = Robolectric.buildActivity(LiveActivity::class.java,
                Intent(MediaContract.PLAY).putExtra(MediaContract.EXTRA_VERSION, version)).create()
            assertTrue(controller.get().isFinishing)
            controller.destroy()
        }
    }
    @Test fun playRejectsUnknownActions() {
        val controller = Robolectric.buildActivity(LiveActivity::class.java, Intent(Intent.ACTION_VIEW)).create()
        assertTrue(controller.get().isFinishing)
        controller.destroy()
    }
    @Test fun settingsRejectsFutureVersion() {
        val controller = Robolectric.buildActivity(LiveSettingsActivity::class.java,
            Intent(MediaContract.SETTINGS).putExtra(MediaContract.EXTRA_VERSION, 2)).create()
        assertTrue(controller.get().isFinishing)
        controller.destroy()
    }
}
