package app.orchard.tv.wallpaper

import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

/** Bing's fullstartdate is UTC; startdate is an identifier, not the local display day. */
internal fun dailyIsCurrent(starts: List<String>, now: Long): Boolean {
    val format = SimpleDateFormat("yyyyMMddHHmm", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
        isLenient = false
    }
    val newest = starts.mapNotNull { start ->
        start.takeIf { it.length == 12 && it.all { c -> c in '0'..'9' } }
            ?.let { runCatching { format.parse(it)?.time }.getOrNull() }
            ?.takeIf { it <= now }
    }.maxOrNull() ?: return false
    return now - newest < 24 * 60 * 60 * 1000L
}
