package app.orchard.tv.cinema

import org.junit.Test
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Explicit local opt-in; never writes a household record or exposes its response in output. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk=[23],manifest=Config.NONE)
class MoonTvLiveTest {
    @Test fun deployedAuthenticationSearchDetailAndResolution() {
        val server=System.getenv("AETHER_MOON_SERVER_FILE")
        val authFile=System.getenv("AETHER_MOON_AUTH_FILE")
        assumeTrue(server!=null && authFile!=null)
        val auth=org.json.JSONObject(java.io.File(authFile!!).readText())
        val p=MoonTvProvider()
        val a=p.login(java.io.File(server!!).readText().trim(),auth.getString("username"),auth.getString("password"),"cinema-l0-readonly")
        assertEquals(Backend.MOONTV,a.backend)
        assertTrue(a.token.isNotEmpty())
        val page=p.items(a,search="地球")
        assertTrue(page.total>0)
        assertTrue(page.items.size<=48)
        val episodes=p.items(a,parent=page.items.first().id)
        assertTrue(episodes.items.isNotEmpty())
        val m=p.detail(a,episodes.items.first().id)
        val playback=p.negotiate(a,m,0)
        assertEquals("DirectPlay",playback.method)
        assertTrue(java.net.URI(playback.url).scheme in listOf("http","https"))
        assertTrue(p.resume(a).size<=24)
    }
    @Test fun opaqueIdentifiersRoundTripWithoutStreamUrls() {
        val id=MoonTvProvider().id("provider+one","12345",2)
        assertFalse(id.contains("http"));assertFalse(id.contains("provider"))
        val decoded=org.json.JSONObject(String(android.util.Base64.decode(id,android.util.Base64.URL_SAFE),Charsets.UTF_8))
        assertEquals("provider+one",decoded.getString("s"));assertEquals(2,decoded.getInt("e"))
    }
}
