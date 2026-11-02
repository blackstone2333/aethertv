package app.orchard.tv.guard

import android.Manifest
import android.content.ContentResolver
import android.content.Context
import android.content.ContextWrapper
import android.os.Build
import android.provider.Settings
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.util.ReflectionHelpers

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [23])
class GuardSetupAdbTest {
    private val context: Context = RuntimeEnvironment.getApplication()

    @Before
    fun prepareOptedInAdb() {
        shadowOf(RuntimeEnvironment.getApplication()).grantPermissions(Manifest.permission.WRITE_SECURE_SETTINGS)
        context.getSharedPreferences("system", Context.MODE_PRIVATE).edit().putBoolean("keep_adb", true).commit()
        ReflectionHelpers.setField(GuardSetup, "adbDanced", false)
        ReflectionHelpers.setField(GuardSetup, "adbDancing", false)
        // An invalid port fails locally, without connecting to a device or another process.
        GuardSetup.adbProbePort = -1
    }

    @Test
    fun preMarshmallowLeavesEnabledAdbRunning() {
        // Robolectric 4.17 starts at SDK23. This probes only the older SDK guard, not Android5 runtime.
        for (sdk in listOf(21, 22)) withSdk(sdk) {
            Settings.Global.putInt(context.contentResolver, Settings.Global.ADB_ENABLED, 1)
            val backgroundAccess = CountDownLatch(1)
            val caller = Thread.currentThread()
            val trackedContext = object : ContextWrapper(context) {
                override fun getContentResolver(): ContentResolver {
                    if (Thread.currentThread() !== caller) backgroundAccess.countDown()
                    return super.getContentResolver()
                }
            }
            GuardSetup.ensureAdb(trackedContext)
            assertFalse("SDK$sdk must not start the restart worker", backgroundAccess.await(250, TimeUnit.MILLISECONDS))
            assertFalse(ReflectionHelpers.getField(GuardSetup, "adbDanced"))
            assertEquals(1, Settings.Global.getInt(context.contentResolver, Settings.Global.ADB_ENABLED))
        }
    }

    @Test
    fun preMarshmallowStillRestoresDisabledAdbWhenOptedIn() {
        for (sdk in listOf(21, 22)) withSdk(sdk) {
            Settings.Global.putInt(context.contentResolver, Settings.Global.ADB_ENABLED, 0)
            GuardSetup.ensureAdb(context)
            assertEquals(1, Settings.Global.getInt(context.contentResolver, Settings.Global.ADB_ENABLED))
        }
    }

    @Test
    fun marshmallowKeepsItsBoundedBootRestart() {
        Settings.Global.putInt(context.contentResolver, Settings.Global.ADB_ENABLED, 1)
        val workerStarted = CountDownLatch(1)
        val caller = Thread.currentThread()
        val trackedContext = object : ContextWrapper(context) {
            override fun getContentResolver(): ContentResolver {
                if (Thread.currentThread() !== caller) workerStarted.countDown()
                return super.getContentResolver()
            }
        }
        GuardSetup.ensureAdb(trackedContext)
        assertTrue(workerStarted.await(2, TimeUnit.SECONDS))
        val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(3)
        while (ReflectionHelpers.getField<Boolean>(GuardSetup, "adbDancing") && System.nanoTime() < deadline) {
            Thread.sleep(10)
        }
        assertFalse(ReflectionHelpers.getField(GuardSetup, "adbDancing"))
        assertTrue(ReflectionHelpers.getField(GuardSetup, "adbDanced"))
        assertEquals(1, Settings.Global.getInt(context.contentResolver, Settings.Global.ADB_ENABLED))
    }

    private fun withSdk(sdk: Int, block: () -> Unit) {
        val original = Build.VERSION.SDK_INT
        try {
            ReflectionHelpers.setStaticField(Build.VERSION::class.java, "SDK_INT", sdk)
            block()
        } finally {
            ReflectionHelpers.setStaticField(Build.VERSION::class.java, "SDK_INT", original)
        }
    }
}
