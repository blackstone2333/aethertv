package app.orchard.tv.guard

import app.orchard.tv.guard.UsageFront.MOVE_TO_BACKGROUND
import app.orchard.tv.guard.UsageFront.MOVE_TO_FOREGROUND
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class UsageFrontTest {
    @Test fun futureEventsAreIgnoredAndNewestRealFrontWins() {
        val now = 10_000L
        val picker = UsageFront.Picker(now + UsageFront.FUTURE_SLACK_MS)
        picker.add(MOVE_TO_FOREGROUND, "com.player", ".Main", now - 2_000L)
        picker.add(MOVE_TO_FOREGROUND, "com.stock", ".Home", now)
        picker.add(MOVE_TO_FOREGROUND, "com.stale", ".Old", now + 86_400_000L)
        val front = picker.result(now = now)!!
        assertEquals("com.stock", front.pkg)
        assertEquals(now, front.at)
        assertFalse(front.closed)
        assertTrue(picker.skippedFuture)
    }

    @Test fun olderEventsDoNotReplaceNewerFront() {
        val picker = UsageFront.Picker()
        picker.add(MOVE_TO_FOREGROUND, "com.new", ".New", 500L)
        picker.add(MOVE_TO_FOREGROUND, "com.old", ".Old", 200L)
        assertEquals("com.new", picker.result()!!.pkg)
    }

    @Test fun settledBackgroundIsClosedButTransientPauseIsOpen() {
        val picker = UsageFront.Picker()
        picker.add(MOVE_TO_FOREGROUND, "com.player", ".Main", 100L)
        picker.add(MOVE_TO_BACKGROUND, "com.player", ".Main", 200L)
        assertFalse(picker.result(now = 250L, activitySettleMs = 1_000L)!!.closed)
        assertTrue(picker.result(now = 1_500L, activitySettleMs = 1_000L)!!.closed)
    }

    @Test fun openEndedQueryFallsBackWhenEmpty() {
        val calls = mutableListOf<Pair<Long, Long>>()
        val value = UsageFront.queryEvents(
            begin = -1L,
            now = 10_000L,
            query = { begin, end ->
                calls += begin to end
                if (end == Long.MAX_VALUE) emptyList() else listOf("event")
            },
            hasNext = { it.isNotEmpty() },
        )
        assertEquals(listOf("event"), value)
        assertEquals(listOf(0L to Long.MAX_VALUE, 0L to 10_000L), calls)
    }

    @Test fun noEventsAreUnknown() {
        assertNull(UsageFront.Picker().result())
    }
}
