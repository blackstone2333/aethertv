package app.orchard.tv.guard

import android.content.Context
import android.os.PowerManager
import android.os.UserManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.util.ReflectionHelpers

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class HomeGuardControlCenterTest {
    private fun checkTakeover(resumed: Boolean, staleStockWindow: Boolean) {
        val controller = Robolectric.buildService(HomeGuardService::class.java).create()
        val service = controller.get()
        shadowOf(service.getSystemService(Context.USER_SERVICE) as UserManager).setUserUnlocked(true)
        shadowOf(service.getSystemService(Context.POWER_SERVICE) as PowerManager).setIsInteractive(true)
        if (staleStockWindow) {
            ReflectionHelpers.setField(service, "stockHomes", mapOf("test.stock" to setOf("Home")))
            ReflectionHelpers.setField(service, "lastWindow", "test.stock" to "Home")
        }
        if (resumed) HomeGuardService.onAerialResumed() else HomeGuardService.onAerialPaused()
        try {
            ReflectionHelpers.callInstanceMethod<Unit>(service, "takeOverIfStockHomeInFront",
                ReflectionHelpers.ClassParameter.from(String::class.java, "guard rebound"),
                ReflectionHelpers.ClassParameter.from(Boolean::class.javaPrimitiveType, false))
            val launched = shadowOf(service).nextStartedActivity
            if (resumed) assertNull("Already resumed Home must retain its open control page", launched)
            else assertEquals(service.packageName, launched?.component?.packageName)
        } finally {
            HomeGuardService.onAerialPaused()
            controller.destroy()
        }
    }

    @Test fun rebindingWithNoWindowDoesNotResetVisibleControls() = checkTakeover(true, false)
    @Test fun resumedActivityWinsOverStaleStockWindow() = checkTakeover(true, true)
    @Test fun realStockLauncherCanStillBeReplacedAfterHomePauses() = checkTakeover(false, true)
}
