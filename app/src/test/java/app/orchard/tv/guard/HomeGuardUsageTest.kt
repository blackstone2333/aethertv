package app.orchard.tv.guard

import android.app.AppOpsManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.os.Process
import android.os.SystemClock
import android.os.UserManager
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent
import kotlinx.coroutines.Job
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.android.controller.ServiceController
import org.robolectric.annotation.Config
import org.robolectric.annotation.Implementation
import org.robolectric.annotation.Implements
import org.robolectric.annotation.LooperMode
import org.robolectric.shadow.api.Shadow
import org.robolectric.shadows.ShadowUsageStatsManager
import org.robolectric.util.ReflectionHelpers
import java.time.Duration
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.locks.LockSupport

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [23], shadows = [HomeGuardUsageTest.BlockingUsageStats::class])
@LooperMode(LooperMode.Mode.PAUSED)
class HomeGuardUsageTest {
    private lateinit var controller: ServiceController<HomeGuardService>
    private lateinit var service: HomeGuardService
    private lateinit var usage: BlockingUsageStats
    private var destroyed = false

    @Before fun setUp() {
        controller = Robolectric.buildService(HomeGuardService::class.java).create()
        service = controller.get()
        ReflectionHelpers.setStaticField(HomeGuardService::class.java, "instance", service)
        HomeGuardService.onAerialPaused()
        ReflectionHelpers.setField(service, "stockHomes", mapOf(STOCK_PACKAGE to setOf(STOCK_HOME)))
        shadowOf(service.getSystemService(Context.POWER_SERVICE) as PowerManager).setIsInteractive(true)
        permitUsage(true)
        usage = Shadow.extract(service.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager)
        addEvent(STOCK_PACKAGE, STOCK_HOME, System.currentTimeMillis() - 1_000)
    }

    @After fun tearDown() {
        usage.release.countDown()
        if (!destroyed) controller.destroy()
        awaitQueryCompletion()
        HomeGuardService.onAerialPaused()
    }

    @Test fun blockedBinderLeavesMainLooperUsableAndPollsDoNotAccumulate() {
        startQuery()
        var mainCallbackRan = false
        Handler(Looper.getMainLooper()).post { mainCallbackRan = true }
        repeat(4) { poll() }
        shadowOf(Looper.getMainLooper()).idle()
        assertTrue(mainCallbackRan)
        assertEquals(1, usage.calls.get())

        finishQuery()
        assertHomeStarted()
        poll()
        awaitQueryCompletion()
        assertEquals(2, usage.calls.get())
        assertNull("One usage event must only bring Home back once", shadowOf(service).nextStartedActivity)
    }

    @Test fun newestAppEventWinsOverOlderStockHome() {
        addEvent("test.player", "test.player.Video", System.currentTimeMillis() - 500)
        startQuery()
        finishQuery()
        assertNull(shadowOf(service).nextStartedActivity)
    }

    @Test fun anotherScreenInTheStockPackageIsLeftAlone() {
        addEvent(STOCK_PACKAGE, "test.stock.Children", System.currentTimeMillis() - 500)
        startQuery()
        finishQuery()
        assertNull(shadowOf(service).nextStartedActivity)
    }

    @Test fun failedQueryCanBeRetried() {
        usage.failNext = true
        startQuery()
        finishQuery()
        assertNull(shadowOf(service).nextStartedActivity)
        poll()
        awaitQueryCompletion()
        assertEquals(2, usage.calls.get())
        assertHomeStarted()
    }

    @Test fun permissionAndVisibleHomeGateQueries() {
        permitUsage(false)
        poll()
        assertNull(query())
        permitUsage(true)
        HomeGuardService.onAerialResumed()
        poll()
        assertNull(query())
        assertEquals(0, usage.calls.get())

        HomeGuardService.onAerialPaused()
        startQuery()
        finishQuery()
        assertHomeStarted()
    }

    @Test fun revokedPermissionDropsTheResultAndLaterGrantRecovers() {
        startQuery()
        permitUsage(false)
        finishQuery()
        assertNull(shadowOf(service).nextStartedActivity)
        permitUsage(true)
        poll()
        awaitQueryCompletion()
        assertHomeStarted()
    }

    @Test fun aNewWindowSupersedesTheUsageSnapshot() {
        startQuery()
        val window = "test.player" to "test.player.Video"
        ReflectionHelpers.getField<MutableMap<Pair<String, String>, Boolean>>(service, "activityCache")[window] = true
        val event = AccessibilityEvent.obtain(AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED).apply {
            packageName = window.first
            className = window.second
        }
        service.onAccessibilityEvent(event)
        event.recycle()
        finishQuery()
        assertNull(shadowOf(service).nextStartedActivity)
    }

    @Test fun inputSupersedesTheUsageSnapshot() {
        startQuery()
        ReflectionHelpers.callInstanceMethod<Boolean>(service, "onKeyEvent",
            ReflectionHelpers.ClassParameter.from(KeyEvent::class.java,
                KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_RIGHT)))
        finishQuery()
        assertNull(shadowOf(service).nextStartedActivity)
    }

    @Test fun resumedThenPausedHomeStillSupersedesTheSnapshot() {
        startQuery()
        HomeGuardService.onAerialResumed()
        HomeGuardService.onAerialPaused()
        finishQuery()
        assertNull(shadowOf(service).nextStartedActivity)
    }

    @Test fun screenOffDropsAnInFlightResult() {
        startQuery()
        ReflectionHelpers.getField<BroadcastReceiver>(service, "screenOn")
            .onReceive(service, Intent(Intent.ACTION_SCREEN_OFF))
        finishQuery()
        assertNull(shadowOf(service).nextStartedActivity)
    }

    @Test fun intentionalStockVisitPreventsTakeover() {
        startQuery()
        HomeGuardService.allowStockLauncher()
        finishQuery()
        poll()
        assertEquals(1, usage.calls.get())
        assertNull(shadowOf(service).nextStartedActivity)
    }

    @Test fun overAgeResultIsDiscardedWithoutQueueingMoreWork() {
        startQuery()
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(2_001))
        finishQuery()
        assertEquals(1, usage.calls.get())
        assertNull(shadowOf(service).nextStartedActivity)
    }

    @Test fun rebindWaitsForTheCancelledBinderCallToUnwind() {
        startQuery()
        service.onUnbind(null)
        assertFalse("Cancellation cannot finish a blocked Binder call", query()!!.isCompleted)
        // Isolate the periodic path from the unrelated early-boot takeover timers.
        SystemClock.sleep(TimeUnit.MINUTES.toMillis(4))
        shadowOf(service.application).grantPermissions(
            "${service.packageName}.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION",
        )
        ReflectionHelpers.callInstanceMethod<Unit>(service, "onServiceConnected")
        ReflectionHelpers.setField(service, "stockHomes", mapOf(STOCK_PACKAGE to setOf(STOCK_HOME)))
        shadowOf(Looper.getMainLooper()).idle()
        assertEquals(1, usage.calls.get())
        finishQuery()
        assertNull(shadowOf(service).nextStartedActivity)

        addEvent(STOCK_PACKAGE, STOCK_HOME, System.currentTimeMillis() - 1)
        poll()
        awaitQueryCompletion()
        assertEquals(2, usage.calls.get())
        assertHomeStarted()
    }

    @Test fun destroyedBindingCannotDeliverIntoAReplacementService() {
        startQuery()
        controller.destroy()
        destroyed = true
        val replacement = Robolectric.buildService(HomeGuardService::class.java).create()
        ReflectionHelpers.setStaticField(HomeGuardService::class.java, "instance", replacement.get())
        try {
            finishQuery()
            poll()
            assertEquals(1, usage.calls.get())
            assertNull(shadowOf(service).nextStartedActivity)
            assertNull(shadowOf(replacement.get()).nextStartedActivity)
        } finally {
            replacement.destroy()
        }
    }

    @Test @Config(sdk = [26]) fun lockedUserDoesNotStartAQuery() {
        val user = shadowOf(service.getSystemService(Context.USER_SERVICE) as UserManager)
        user.setUserUnlocked(false)
        poll()
        assertNull(query())
        user.setUserUnlocked(true)
        startQuery()
        finishQuery()
        assertHomeStarted()
    }

    private fun permitUsage(allowed: Boolean) {
        shadowOf(service.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager).setMode(
            AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), service.packageName,
            if (allowed) AppOpsManager.MODE_ALLOWED else AppOpsManager.MODE_IGNORED,
        )
    }

    private fun addEvent(pkg: String, cls: String, at: Long) {
        usage.addEvent(ShadowUsageStatsManager.EventBuilder.buildEvent()
            .setPackage(pkg).setClass(cls).setTimeStamp(at)
            .setEventType(UsageEvents.Event.MOVE_TO_FOREGROUND).build())
    }

    private fun poll() {
        val runnable = ReflectionHelpers.getField<Runnable>(service, "foregroundCheck")
        ReflectionHelpers.getField<Handler>(service, "handler").removeCallbacks(runnable)
        runnable.run()
    }

    private fun query(): Job? = ReflectionHelpers.getField(service, "foregroundQuery")

    private fun startQuery() {
        poll()
        assertTrue("IO query did not start", usage.entered.await(5, TimeUnit.SECONDS))
        assertFalse("Usage-stats Binder must not run on the main looper", usage.queriedOnMain)
    }

    private fun finishQuery() {
        usage.release.countDown()
        awaitQueryCompletion()
        assertFalse("Test query timed out before release", usage.timedOut)
    }

    private fun awaitQueryCompletion() {
        val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5)
        while (query()?.isCompleted == false && System.nanoTime() < deadline) {
            shadowOf(Looper.getMainLooper()).idle()
            LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(1))
        }
        assertTrue("IO query did not complete", query()?.isCompleted != false)
    }

    private fun assertHomeStarted() {
        val intent = shadowOf(service).nextStartedActivity
        assertEquals("com.dangbei.tvlauncher", intent?.component?.packageName)
        assertEquals("app.orchard.tv.MainActivity", intent?.component?.className)
    }

    @Implements(UsageStatsManager::class)
    class BlockingUsageStats : ShadowUsageStatsManager() {
        val entered = CountDownLatch(1)
        val release = CountDownLatch(1)
        val calls = AtomicInteger()
        @Volatile var queriedOnMain = false
        @Volatile var timedOut = false
        @Volatile var failNext = false

        @Implementation public override fun queryEvents(beginTime: Long, endTime: Long): UsageEvents {
            calls.incrementAndGet()
            queriedOnMain = Looper.myLooper() == Looper.getMainLooper()
            val snapshot = super.queryEvents(beginTime, endTime)
            entered.countDown()
            if (!release.await(5, TimeUnit.SECONDS)) timedOut = true
            if (failNext) {
                failNext = false
                throw IllegalStateException("Synthetic usage service failure")
            }
            return snapshot
        }
    }

    private companion object {
        const val STOCK_PACKAGE = "test.stock"
        const val STOCK_HOME = "test.stock.Home"
    }
}
