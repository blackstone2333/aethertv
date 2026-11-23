package app.orchard.tv.guard

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager

/**
 * Small, deterministic adapter around usage events. Some TV firmwares keep the active daily
 * bucket dated in the future; callers need the open-ended query while still rejecting stale events.
 */
internal object UsageFront {
    const val MOVE_TO_FOREGROUND = 1 // UsageEvents.Event.MOVE_TO_FOREGROUND / ACTIVITY_RESUMED
    const val MOVE_TO_BACKGROUND = 2 // UsageEvents.Event.MOVE_TO_BACKGROUND / ACTIVITY_PAUSED
    const val FUTURE_SLACK_MS = 5_000L

    /** Includes the current in-memory bucket, falling back for firmwares that reject MAX_VALUE. */
    fun queryEvents(usm: UsageStatsManager, begin: Long, now: Long): UsageEvents? =
        queryEvents(begin, now, usm::queryEvents, UsageEvents::hasNextEvent)

    internal inline fun <T> queryEvents(
        begin: Long,
        now: Long,
        query: (Long, Long) -> T?,
        hasNext: (T) -> Boolean,
    ): T? {
        val start = begin.coerceAtLeast(0L)
        val open = query(start, Long.MAX_VALUE)
        if (open != null && hasNext(open)) return open
        return query(start, now)
    }

    data class Front(val pkg: String, val cls: String, val at: Long, val closed: Boolean)

    class Picker(private val maxAt: Long = Long.MAX_VALUE) {
        private var front: Front? = null
        private var pausedAt: Long? = null
        var skippedFuture: Boolean = false
            private set

        fun add(type: Int, pkg: String, cls: String?, at: Long) {
            if (at > maxAt) {
                skippedFuture = true
                return
            }
            if (cls == null) return
            val current = front
            when (type) {
                MOVE_TO_FOREGROUND -> if (current == null || at >= current.at) {
                    front = Front(pkg, cls, at, closed = false)
                    pausedAt = null
                }
                MOVE_TO_BACKGROUND -> if (current != null && current.pkg == pkg && current.cls == cls && at >= current.at) {
                    front = current.copy(closed = true)
                    pausedAt = at
                }
            }
        }

        fun result(now: Long = Long.MAX_VALUE, activitySettleMs: Long = 0): Front? {
            val current = front ?: return null
            val paused = pausedAt
            return if (current.closed && paused != null && now - paused < activitySettleMs) {
                current.copy(closed = false)
            } else current
        }
    }
}
