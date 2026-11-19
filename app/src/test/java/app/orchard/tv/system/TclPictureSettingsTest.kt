package app.orchard.tv.system

import android.content.ComponentName
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageInfo
import android.provider.Settings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [23])
class TclPictureSettingsTest {
    private val context: Context = RuntimeEnvironment.getApplication()
    private val pm = shadowOf(context.packageManager)

    @Test
    fun tclPicturePanelUsesTheRemotesStringExtras() {
        installTcl()
        val caller = RecordingContext()
        assertTrue(SystemProbe.openPictureSettings(caller))
        val intent = caller.services.single()
        assertEquals("com.tcl.settings.SHOW_WINDOW", intent.action)
        assertEquals("com.tcl.settings", intent.`package`)
        assertEquals("Settings", intent.getStringExtra("Type"))
        assertEquals("1", intent.getStringExtra("flag"))
        assertTrue(caller.activities.isEmpty())
    }

    @Test
    fun failedOrRejectedTclServiceKeepsTheDisplaySettingsFallback() {
        installTcl()
        val display = ComponentName("test.display", "test.display.Settings")
        pm.addActivityIfNotPresent(display).exported = true
        pm.addIntentFilterForActivity(display, IntentFilter(Settings.ACTION_DISPLAY_SETTINGS))
        for (throws in listOf(false, true)) {
            val caller = RecordingContext(serviceResult = null, serviceThrows = throws)
            assertTrue(SystemProbe.openPictureSettings(caller))
            assertEquals(1, caller.services.size)
            assertEquals(Settings.ACTION_DISPLAY_SETTINGS, caller.activities.single().action)
        }
    }

    @Test
    fun xiaomiBacklightBroadcastKeepsPriorityOverTcl() {
        installTcl()
        val receiver = ComponentName("com.xiaomi.mitv.settings", "com.xiaomi.mitv.settings.SettingsBroadcastReceiver")
        pm.addReceiverIfNotPresent(receiver).exported = true
        pm.addIntentFilterForReceiver(receiver, IntentFilter("com.xiaomi.mitv.settings.BACKLIGHT_POPUP"))
        val caller = RecordingContext()
        assertTrue(SystemProbe.openPictureSettings(caller))
        assertEquals("com.xiaomi.mitv.settings.BACKLIGHT_POPUP", caller.broadcasts.single().action)
        assertTrue(caller.services.isEmpty())
        assertTrue(caller.activities.isEmpty())
    }

    @Test
    fun missingTclDoesNotAttemptItsService() {
        val settings = ComponentName("test.settings", "test.settings.Main")
        pm.addActivityIfNotPresent(settings).exported = true
        pm.addIntentFilterForActivity(settings, IntentFilter(Settings.ACTION_SETTINGS))
        val caller = RecordingContext()
        assertTrue(SystemProbe.openPictureSettings(caller))
        assertTrue(caller.services.isEmpty())
        assertEquals(Settings.ACTION_SETTINGS, caller.activities.single().action)
    }

    private fun installTcl() {
        pm.installPackage(PackageInfo().apply { packageName = "com.tcl.settings" })
    }

    private inner class RecordingContext(
        private val serviceResult: ComponentName? = ComponentName("com.tcl.settings", "com.tcl.settings.ShowWindowService"),
        private val serviceThrows: Boolean = false,
    ) : ContextWrapper(context) {
        val services = mutableListOf<Intent>()
        val activities = mutableListOf<Intent>()
        val broadcasts = mutableListOf<Intent>()

        override fun startService(intent: Intent): ComponentName? {
            services += intent
            if (serviceThrows) throw SecurityException("Service access denied")
            return serviceResult
        }

        override fun startActivity(intent: Intent) { activities += intent }
        override fun sendBroadcast(intent: Intent) { broadcasts += intent }
    }
}
