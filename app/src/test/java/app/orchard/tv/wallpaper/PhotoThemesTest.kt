package app.orchard.tv.wallpaper

import org.junit.Assert.assertEquals
import org.junit.Test

class PhotoThemesTest {
    @Test fun topicsUseCaptionsInBothCatalogueLanguages() {
        assertEquals("animals", PhotoThemes.of("企鹅", "海边的企鹅群"))
        assertEquals("water", PhotoThemes.of("A quiet lake", "Morning in the mountains"))
        assertEquals("city", PhotoThemes.of("Bridge", "城市夜景"))
        assertEquals("space", PhotoThemes.of("Aurora", "Polar lights"))
        assertEquals("other", PhotoThemes.of("A new day", "Photo by a guest"))
    }
}
