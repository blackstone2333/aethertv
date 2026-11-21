package app.orchard.tv.system

import android.Manifest
import android.app.Application
import android.content.ContentResolver
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.PackageInfo
import android.provider.Settings
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [23])
class FengOsBrightnessTest {
    private val context: Context = RuntimeEnvironment.getApplication()

    @Test fun fengOsBlocksAllBrightnessAccessBeforeItReachesSettingsOrVendorCode() {
        assertBlockedWhenInstalled("cn.formovie.fengos.settings")
    }

    @Test fun fengmiLauncherWithoutTheSettingsPanelAlsoBlocksAllBrightnessAccess() {
        assertBlockedWhenInstalled("com.tvfengmi.launcher")
    }

    private fun assertBlockedWhenInstalled(pkg: String) {
        shadowOf(context.packageManager).installPackage(PackageInfo().apply { packageName = pkg })
        val caller = object : ContextWrapper(context) {
            override fun getContentResolver(): ContentResolver = throw AssertionError("FengOS brightness must not access settings")
        }
        assertTrue(SystemProbe.hidesBrightness(caller))
        assertNull(Brightness.level(caller))
        assertNull(Brightness.adjust(caller, 1))
        assertNull(Brightness.adjust(caller, -1))
    }

    @Test fun unrelatedPackagesDoNotSuppressTheExistingBacklightPath() {
        shadowOf(context.packageManager).installPackage(PackageInfo().apply { packageName = "cn.formovie.other" })
        shadowOf(context.packageManager).installPackage(PackageInfo().apply { packageName = "com.tvfengmi.other" })
        shadowOf(context as Application).grantPermissions(Manifest.permission.WRITE_SECURE_SETTINGS)
        val resolver = context.contentResolver
        Settings.System.putInt(resolver, Settings.System.SCREEN_BRIGHTNESS, 128)
        Settings.Global.putInt(resolver, "picture_backlight", 50)
        assertFalse(SystemProbe.hidesBrightness(context))
        assertNotNull(Brightness.level(context))
        Brightness.adjust(context, 1)
        assertEquals(55, Settings.Global.getInt(resolver, "picture_backlight"))
        assertEquals(128, Settings.System.getInt(resolver, Settings.System.SCREEN_BRIGHTNESS))
    }
}
