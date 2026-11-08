package app.orchard.tv.system

import android.content.ComponentName
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ActivityInfo
import android.content.pm.ApplicationInfo
import android.content.pm.ResolveInfo
import android.content.pm.ServiceInfo
import android.media.tv.TvContract
import android.media.tv.TvInputInfo
import android.media.tv.TvInputManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.Implementation
import org.robolectric.annotation.Implements
import org.robolectric.shadow.api.Shadow
import org.robolectric.util.ReflectionHelpers

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [23], shadows = [InputRoutingTest.InputManager::class])
class InputRoutingTest {
    private val context: Context = RuntimeEnvironment.getApplication()
    private val pm = shadowOf(context.packageManager)
    private val mstar = ComponentName("test.mstar", "test.mstar.SourcePanel")
    private val jmgo = ComponentName("com.jmgo.tv", "com.jmgo.tv.AmlogicTvActivity")

    @Test fun mstarFallbackStartsTheResolvedComponentWithANewTask() {
        registerMstar()
        val caller = RecordingContext()
        assertTrue(SystemProbe.openInputPicker(caller))
        assertEquals(1, caller.activities.size)
        val intent = caller.activities.single()
        assertEquals(MSTAR_ACTION, intent.action)
        assertEquals(mstar, intent.component)
        assertEquals(Intent.FLAG_ACTIVITY_NEW_TASK, intent.flags)
        assertNull(intent.extras)
    }

    @Test fun unavailableOrInaccessibleMstarPanelIsNotStarted() {
        val caller = RecordingContext()
        assertFalse(SystemProbe.openInputPicker(caller))
        val activity = registerMstar()
        for (blocked in listOf<(ActivityInfo) -> Unit>(
            { it.exported = false }, { it.enabled = false },
            { it.applicationInfo.enabled = false }, { it.permission = SOURCE_PERMISSION },
        )) {
            activity.exported = true
            activity.enabled = true
            activity.applicationInfo.enabled = true
            activity.permission = null
            blocked(activity)
            assertFalse(SystemProbe.openInputPicker(caller))
        }
        assertTrue(caller.activities.isEmpty())
    }

    @Test fun grantedMstarPermissionIsAcceptedAndStartFailureIsReported() {
        registerMstar().permission = SOURCE_PERMISSION
        shadowOf(RuntimeEnvironment.getApplication()).grantPermissions(SOURCE_PERMISSION)
        assertTrue(SystemProbe.openInputPicker(RecordingContext()))
        assertFalse(SystemProbe.openInputPicker(RecordingContext(activityFails = true)))
    }

    @Test fun hisensePanelKeepsPriorityAndFailureAllowsMstarFallback() {
        registerMstar()
        pm.addServiceIfNotPresent(ComponentName("com.hisense.tv.inputs", "com.hisense.tv.InputsUIService"))
            .exported = true
        val first = RecordingContext()
        assertTrue(SystemProbe.openInputPicker(first))
        assertEquals("com.hisense.tv.start_inputs", first.services.single().action)
        assertTrue(first.activities.isEmpty())
        val failed = RecordingContext(serviceFails = true)
        assertTrue(SystemProbe.openInputPicker(failed))
        assertEquals(mstar, failed.activities.single().component)
    }

    @Test fun tclPanelKeepsPriorityAndNullServiceResultAllowsMstarFallback() {
        registerMstar()
        val component = ComponentName("com.tcl.settings", "com.tcl.settings.InputService")
        val service = pm.addServiceIfNotPresent(component).apply { exported = true }
        pm.addResolveInfoForIntent(Intent("com.tcl.insrc.SHOW_WINDOW").setPackage("com.tcl.settings"),
            ResolveInfo().apply { serviceInfo = service })
        val first = RecordingContext()
        assertTrue(SystemProbe.openInputPicker(first))
        assertEquals("com.tcl.insrc.SHOW_WINDOW", first.services.single().action)
        assertTrue(first.activities.isEmpty())
        val failed = RecordingContext(serviceReturnsNull = true)
        assertTrue(SystemProbe.openInputPicker(failed))
        assertEquals(mstar, failed.activities.single().component)
    }

    @Test fun jmgoUsesTheInputServicePortAndPreservesSourceMetadata() {
        registerJmgo()
        val caller = RecordingContext()
        setInputs(input())
        val source = SystemProbe.sources(caller).single()
        assertEquals(INPUT_ID, source.id)
        assertEquals("HDMI 3", source.label)
        assertEquals(SourceKind.Hdmi, source.kind)
        assertTrue(source.connected)
        assertEquals(jmgo, source.intent.component)
        assertEquals("HDMI3", source.intent.getStringExtra("inputSrc"))
        assertEquals(Intent.FLAG_ACTIVITY_NEW_TASK, source.intent.flags)
        assertTrue(caller.activities.isEmpty())
        assertTrue(caller.services.isEmpty())
    }

    @Test fun jmgoRejectsMissingOrInaccessiblePlayers() {
        setInputs(input())
        assertNull(SystemProbe.sources(context).single().intent.component)
        val activity = registerJmgo()
        for (blocked in listOf<(ActivityInfo) -> Unit>(
            { it.exported = false }, { it.enabled = false },
            { it.applicationInfo.enabled = false }, { it.permission = SOURCE_PERMISSION },
        )) {
            activity.exported = true
            activity.enabled = true
            activity.applicationInfo.enabled = true
            activity.permission = null
            blocked(activity)
            assertNull(SystemProbe.sources(context).single().intent.component)
        }
        shadowOf(RuntimeEnvironment.getApplication()).grantPermissions(SOURCE_PERMISSION)
        assertEquals(jmgo, SystemProbe.sources(context).single().intent.component)
    }

    @Test fun jmgoDoesNotGuessPortsOrRouteOtherInputTypes() {
        registerJmgo()
        for ((id, type) in listOf(
            INPUT_ID to TvInputInfo.TYPE_COMPOSITE,
            INPUT_ID.replace("Hdmi3", "Hdmi0") to TvInputInfo.TYPE_HDMI,
            INPUT_ID.replace("Hdmi3", "Hdmi") to TvInputInfo.TYPE_HDMI,
            INPUT_ID.replace("Hdmi3", "Other3") to TvInputInfo.TYPE_HDMI,
            INPUT_ID.substringBefore("/HW") to TvInputInfo.TYPE_HDMI,
        )) {
            setInputs(input(id = id, type = type))
            assertNull("Unexpected JMGO route for $id / $type", SystemProbe.sources(context).single().intent.component)
        }
    }

    @Test fun genericCecAndDroidLogicRoutesKeepPriorityOverJmgo() {
        registerJmgo()
        val parent = input()
        val child = input(id = "$INPUT_ID/HDMI050005", parent = INPUT_ID, label = "Apple TV")
        setInputs(parent, child)
        val generic = ComponentName("test.generic", "test.generic.Player")
        pm.addActivityIfNotPresent(generic).exported = true
        pm.addIntentFilterForActivity(generic, IntentFilter(Intent.ACTION_VIEW).apply { addDataScheme("content") })
        val source = SystemProbe.sources(context).single()
        assertEquals(TvContract.buildChannelUriForPassthroughInput(child.id), source.intent.data)
        assertNull(source.intent.component)
        assertEquals("Apple TV", source.deviceName)

        pm.removeActivity(generic)
        val droidLogic = ComponentName("com.droidlogic.tvsource", "com.droidlogic.tvsource.DroidLogicTv")
        pm.addActivityIfNotPresent(droidLogic).exported = true
        val fallback = SystemProbe.sources(context).single()
        assertEquals(droidLogic, fallback.intent.component)
        assertEquals(INPUT_ID, fallback.intent.getStringExtra("id"))
    }

    private fun registerMstar(): ActivityInfo = pm.addActivityIfNotPresent(mstar).apply {
        exported = true
        pm.addIntentFilterForActivity(mstar, IntentFilter(MSTAR_ACTION))
    }

    private fun registerJmgo(): ActivityInfo = pm.addActivityIfNotPresent(jmgo).apply { exported = true }

    private fun setInputs(vararg infos: TvInputInfo) {
        Shadow.extract<InputManager>(context.getSystemService(Context.TV_INPUT_SERVICE)).inputs = infos.toList()
    }

    private fun input(
        id: String = INPUT_ID,
        type: Int = TvInputInfo.TYPE_HDMI,
        parent: String? = null,
        label: String = "HDMI 3",
    ): TvInputInfo {
        val service = ResolveInfo().apply {
            serviceInfo = ServiceInfo().apply {
                packageName = "test.inputs"
                name = "test.inputs.Service"
                applicationInfo = ApplicationInfo().apply { packageName = "test.inputs" }
            }
        }
        return ReflectionHelpers.callConstructor(TvInputInfo::class.java,
            ReflectionHelpers.ClassParameter.from(ResolveInfo::class.java, service),
            ReflectionHelpers.ClassParameter.from(String::class.java, id),
            ReflectionHelpers.ClassParameter.from(String::class.java, parent),
            ReflectionHelpers.ClassParameter.from(Int::class.javaPrimitiveType, type),
            ReflectionHelpers.ClassParameter.from(Boolean::class.javaPrimitiveType, true),
        ).also { ReflectionHelpers.setField(it, "mLabel", label) }
    }

    private inner class RecordingContext(
        private val serviceFails: Boolean = false,
        private val serviceReturnsNull: Boolean = false,
        private val activityFails: Boolean = false,
    ) : ContextWrapper(context) {
        val services = mutableListOf<Intent>()
        val activities = mutableListOf<Intent>()

        override fun startService(intent: Intent): ComponentName? {
            services += intent
            if (serviceFails) throw SecurityException("Synthetic service denial")
            return if (serviceReturnsNull) null else intent.component ?: ComponentName("test.services", "Shown")
        }

        override fun startActivity(intent: Intent) {
            if (activityFails) throw SecurityException("Synthetic activity denial")
            activities += intent
        }
    }

    @Implements(TvInputManager::class)
    class InputManager {
        var inputs: List<TvInputInfo> = emptyList()
        @Implementation fun getTvInputList(): List<TvInputInfo> = inputs
        @Implementation fun getInputState(id: String): Int = TvInputManager.INPUT_STATE_CONNECTED
    }

    private companion object {
        const val MSTAR_ACTION = "com.mstar.tv.tvplayer.ui.intent.action.SOURCE_INFO"
        const val SOURCE_PERMISSION = "test.permission.SOURCES"
        const val INPUT_ID = "com.droidlogic.tvinput/.services.Hdmi3InputService/HW19"
    }
}
