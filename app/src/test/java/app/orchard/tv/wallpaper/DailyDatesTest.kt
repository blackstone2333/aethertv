package app.orchard.tv.wallpaper

import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DailyDatesTest {
    private fun utc(text: String) = SimpleDateFormat("yyyyMMddHHmm", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }.parse(text)!!.time

    @Test fun chinaUsesThePreviousDateIdForItsCurrentPhoto() {
        val starts = listOf("202611181600", "202611191600")
        assertTrue(dailyIsCurrent(starts, utc("202611191600")))
        assertTrue(dailyIsCurrent(starts, utc("202611201559")))
        assertFalse(dailyIsCurrent(starts, utc("202611201600")))
    }

    @Test fun expiryDoesNotDependOnTheDevicesTimeZone() {
        val before = TimeZone.getDefault()
        try {
            for (zone in listOf("America/Los_Angeles", "Asia/Shanghai", "Europe/Kyiv")) {
                TimeZone.setDefault(TimeZone.getTimeZone(zone))
                assertTrue(dailyIsCurrent(listOf("202611190800"), utc("202611200759")))
                assertFalse(dailyIsCurrent(listOf("202611190800"), utc("202611200800")))
            }
        } finally { TimeZone.setDefault(before) }
    }

    @Test fun oldOrMalformedIndicesNeedAnotherFetch() {
        for (starts in listOf(emptyList(), listOf(""), listOf("20261119"), listOf("202613191600"),
            listOf("202611311600"), listOf("2026111916xx"))) {
            assertFalse(dailyIsCurrent(starts, utc("202611201200")))
        }
    }

    @Test fun aFuturePhotoCannotKeepAnOldIndexCurrent() {
        val now = utc("202611201200")
        assertFalse(dailyIsCurrent(listOf("202611211600", "202611181600"), now))
        assertTrue(dailyIsCurrent(listOf("202611211600", "202611191600"), now))
    }
}
