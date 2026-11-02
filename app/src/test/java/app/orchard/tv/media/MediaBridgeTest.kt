package app.orchard.tv.media

import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.ApplicationInfo
import android.content.pm.ResolveInfo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [23, 34])
class MediaBridgeTest {
    private val context: Context = RuntimeEnvironment.getApplication()

    @Test
    fun missingCompanionDoesNotProduceAnUnresolvableLaunch() {
        assertNull(MediaBridge.liveIntent(context))
        assertNull(MediaBridge.liveIntent(context, settings = true))
    }

    @Test
    fun installedCompanionReceivesExplicitVersionedEntries() {
        for ((settings, action) in listOf(false to MediaContract.PLAY, true to MediaContract.SETTINGS)) {
            val intent = Intent(action).setPackage(MediaContract.LIVE_PACKAGE)
            val info = ResolveInfo().apply {
                activityInfo = ActivityInfo().apply {
                    name = "app.orchard.tv.live.LiveActivity"
                    packageName = MediaContract.LIVE_PACKAGE
                    enabled = true
                    exported = true
                    applicationInfo = ApplicationInfo().apply { packageName = MediaContract.LIVE_PACKAGE }
                }
            }
            shadowOf(context.packageManager).addResolveInfoForIntent(intent, info)
            val launch = requireNotNull(MediaBridge.liveIntent(context, settings))
            assertEquals(action, launch.action)
            assertEquals(MediaContract.LIVE_PACKAGE, launch.`package`)
            assertEquals(MediaContract.VERSION, launch.getIntExtra(MediaContract.EXTRA_VERSION, -1))
            assertNull(launch.data)
        }
    }
}
