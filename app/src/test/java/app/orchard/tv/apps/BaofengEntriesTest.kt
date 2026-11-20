package app.orchard.tv.apps

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [23])
class BaofengEntriesTest {
    private val context: Context = RuntimeEnvironment.getApplication()
    private val pm = shadowOf(context.packageManager)
    private val player = ComponentName("com.baofengtv.tvplayer", "com.baofengtv.tvplayer.MainActivity")
    private val inputs = ComponentName("com.bftv.fui.biu", "com.bftv.fui.biu.views.activity.FRowActivity")

    @Test fun exportedMainOnlyFirmwareEntriesAreDiscoveredWithoutLauncherCategories() = runBlocking {
        pm.addActivityIfNotPresent(player).exported = true
        pm.addActivityIfNotPresent(inputs).exported = true
        val found = AppCatalog.load(context).map { it.component }
        assertTrue(found.containsAll(listOf(player, inputs)))
    }

    @Test fun disabledOrUnexportedEntriesAreExcluded() = runBlocking {
        pm.addActivityIfNotPresent(player).exported = false
        pm.addActivityIfNotPresent(inputs).apply { exported = true; enabled = false }
        val found = AppCatalog.load(context).map { it.component }
        assertFalse(player in found)
        assertFalse(inputs in found)
    }

    @Test fun normalTvLauncherEntryKeepsPriorityAndOneTilePerPackage() = runBlocking {
        pm.addActivityIfNotPresent(player).exported = true
        val launcher = ComponentName(player.packageName, "com.baofengtv.tvplayer.Launcher")
        pm.addActivityIfNotPresent(launcher).exported = true
        pm.addIntentFilterForActivity(launcher, IntentFilter(Intent.ACTION_MAIN).apply { addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER) })
        val found = AppCatalog.load(context).filter { it.packageName == player.packageName }
        assertEquals(1, found.size)
        assertEquals(launcher, found.single().component)
    }
}
