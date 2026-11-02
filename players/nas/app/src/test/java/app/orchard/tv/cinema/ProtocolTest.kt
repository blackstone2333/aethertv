package app.orchard.tv.cinema

import java.net.ServerSocket
import java.net.Socket
import org.json.JSONObject
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.net.InetSocketAddress
import java.net.URI

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[23],manifest=Config.NONE)
class ProtocolTest {
    private lateinit var server: ServerSocket
    private lateinit var worker: Thread
    private lateinit var base: String
    private val requests=java.util.Collections.synchronizedList(mutableListOf<Triple<String,Map<String,List<String>>,String>>())
    private var mode="direct"
    private var status=200
    private val m=Media("movie-1","Fixture","Movie","A generated test film",2026,1_200_000_000,100_000_000,"tag")
    @Before fun start() {
        server=ServerSocket(0,8,java.net.InetAddress.getByName("127.0.0.1"))
        base="http://127.0.0.1:${server.localPort}"
        worker=Thread {
          while (!server.isClosed) {
            val socket=try {server.accept()} catch (_:Exception) {break}
            socket.use { client ->
            val reader=client.getInputStream().bufferedReader()
            val request=reader.readLine() ?: return@use
            val path=request.split(" ")[1].substringBefore('?')
            val headers=mutableMapOf<String,List<String>>()
            while (true) {
                val line=reader.readLine() ?: break
                if(line.isEmpty()) break
                headers[line.substringBefore(":")]=listOf(line.substringAfter(":").trim())
            }
            val length=headers.entries.firstOrNull{it.key.equals("Content-Length",true)}?.value?.first()?.toInt() ?: 0
            val chars=CharArray(length);var offset=0
            while(offset<length) {val n=reader.read(chars,offset,length-offset);if(n<0) break;offset+=n}
            val body=String(chars)
            requests.add(Triple(path,headers,body))
            if(path=="/redirect") {
                client.getOutputStream().write("HTTP/1.1 302 Found\r\nLocation: $base/captured\r\nContent-Length: 0\r\nConnection: close\r\n\r\n".toByteArray())
                return@use
            }
            val emby=path.startsWith("/emby/")
            val value=when {
                path.endsWith("System/Info/Public") -> obj("ProductName" to if(emby) "Emby Server" else "Jellyfin Server","Version" to if(emby) "4.8.11.0" else "10.10.7")
                path.endsWith("AuthenticateByName") -> obj("AccessToken" to "fixture-token","User" to obj("Id" to "user-1","Name" to "Fixture"))
                path.endsWith("PlaybackInfo") -> if(mode=="error") obj("ErrorCode" to "NotAllowed") else obj("PlaySessionId" to "session-1","MediaSources" to org.json.JSONArray().put(obj("Id" to "source-1","SupportsDirectPlay" to (mode=="direct"),"SupportsDirectStream" to (mode=="remux"),"SupportsTranscoding" to true,"TranscodingUrl" to if(mode=="foreign") "https://foreign.example/stream?api_key=secret" else "/Videos/movie-1/master.m3u8?api_key=fixture-token","MediaStreams" to org.json.JSONArray().put(obj("Type" to "Subtitle","Index" to 2,"IsExternal" to true,"Codec" to "srt","Language" to "eng","DisplayTitle" to "English")))))
                path.endsWith("/Views") || path.endsWith("/Items") || path.endsWith("/Resume") -> obj("Items" to org.json.JSONArray().put(media()),"TotalRecordCount" to 1)
                path.contains("/Sessions/") || path.endsWith("ActiveEncodings") -> JSONObject()
                else -> media()
            }
            val bytes=value.toString().toByteArray()
            client.getOutputStream().write("HTTP/1.1 $status OK\r\nContent-Type: application/json\r\nContent-Length: ${bytes.size}\r\nConnection: close\r\n\r\n".toByteArray()+bytes)
            }
          }
        }.apply {isDaemon=true;start()}
    }
    private fun media()=obj("Id" to m.id,"Name" to m.title,"Type" to "Movie","RunTimeTicks" to m.ticks,"ImageTags" to obj("Primary" to "tag"),"UserData" to obj("PlaybackPositionTicks" to m.resume))
    private fun account(b: Backend=Backend.JELLYFIN)=Account("a",b,base,"user-1","Fixture","fixture-token","fixture-device","10.10.7")
    @After fun stop() {server.close();worker.join(1000)}
    @Test fun jellyfinLoginUsesMediaBrowserAndPw() {
        val a=JellyfinProvider().login(base,"fixture","secret","fixture-device")
        assertEquals("fixture-token",a.token)
        val r=requests.last(); assertEquals("secret",JSONObject(r.third).getString("Pw"))
        assertTrue(r.second.entries.any {it.key.equals("Authorization",true) && it.value.first().contains("MediaBrowser")})
    }
    @Test fun embyLoginUsesEmbyRootHeadersAndLegacyHash() {
        val a=EmbyProvider().login(base,"fixture","secret","fixture-device")
        assertEquals(Backend.EMBY,a.backend)
        val r=requests.last(); assertEquals("/emby/Users/AuthenticateByName",r.first)
        assertTrue(r.second.keys.any {it.equals("X-Emby-Authorization",true)})
        assertEquals("e5e9fa1ba31ecd1ae84f75caaa474f3a663f05f4",JSONObject(r.third).getString("Password"))
    }
    @Test fun embyPrefixIsNotDuplicated() {EmbyProvider().login("$base/emby","f","s","d");assertFalse(requests.any{it.first.contains("/emby/emby")})}
    @Test fun providerMismatchFails() {assertThrows(ProtocolFailure::class.java) {JellyfinProvider().login("$base/emby","f","s","d")}}
    @Test fun listAndResumeParseTicks() {val p=JellyfinProvider();assertEquals(m.resume,p.resume(account()).first().resume);assertEquals(1,p.items(account()).total)}
    @Test fun directPlayUsesAuthenticatedServerStream() {
        val p=JellyfinProvider().negotiate(account(),m,m.resume)
        assertEquals("DirectPlay",p.method);assertTrue(p.url.contains("Static=true"));assertTrue(p.url.contains("MediaSourceId=source-1"));assertTrue(p.url.contains("api_key=fixture-token"));assertEquals(0,p.offsetMillis)
    }
    @Test fun remuxAndTranscodeAreSeparate() {
        mode="remux";assertEquals("DirectStream",JellyfinProvider().negotiate(account(),m,m.resume).method)
        mode="transcode";val p=JellyfinProvider().negotiate(account(),m,m.resume);assertEquals("Transcode",p.method);assertEquals(10_000,p.offsetMillis)
    }
    @Test fun forceConversionDisablesDirectPlayInRequest() {
        mode="transcode";JellyfinProvider().negotiate(account(),m,0,transcode=true)
        assertFalse(JSONObject(requests.last().third).getBoolean("EnableDirectPlay"))
    }
    @Test fun externalSubtitlesUseActualSourceAndIndex() {
        val sub=EmbyProvider().negotiate(account(Backend.EMBY),m,0).subtitles.single()
        assertTrue(sub.url.contains("/emby/Videos/movie-1/source-1/Subtitles/2/Stream.vtt"));assertEquals("eng",sub.language)
    }
    @Test fun foreignNegotiatedUrlRejected() {mode="foreign";assertThrows(ProtocolFailure::class.java) {JellyfinProvider().negotiate(account(),m,0)}}
    @Test fun serverMediaErrorIsReported() {mode="error";assertThrows(ProtocolFailure::class.java) {JellyfinProvider().negotiate(account(),m,0)}}
    @Test fun progressLifecycleIncludesSourceSessionAndPosition() {
        val provider=EmbyProvider();val a=account(Backend.EMBY);val p=provider.negotiate(a,m,0)
        listOf("Playing","Playing/Progress","Playing/Stopped").forEach {provider.report(a,p,it,123456789,true)}
        val events=requests.filter{it.first.contains("Sessions/")}
        assertEquals(3,events.size)
        events.forEach {val o=JSONObject(it.third);assertEquals("session-1",o.getString("PlaySessionId"));assertEquals("source-1",o.getString("MediaSourceId"));assertEquals(123456789,o.getLong("PositionTicks"));assertTrue(o.getBoolean("IsPaused"))}
    }
    @Test fun transcodeStopDeletesOnlyOwnEncodingSession() {
        mode="transcode";val provider=JellyfinProvider();val a=account();val p=provider.negotiate(a,m,0)
        provider.report(a,p,"Playing/Stopped",100,true)
        assertEquals("/Videos/ActiveEncodings",requests.last().first)
    }
    @Test fun expiredTokenHasTypedFailure() {status=401;val e=assertThrows(ProtocolFailure::class.java) {JellyfinProvider().resume(account())};assertEquals("auth",e.reason)}
    @Test fun userServerRootAndOriginValidation() {
        assertEquals("http://server:8096/jellyfin",normalizeServer("http://server:8096/jellyfin/"))
        listOf("file:///tmp/x","https://user:secret@server","https://server?token=x","https://server#x").forEach {assertThrows(IllegalArgumentException::class.java){normalizeServer(it)}}
        assertFalse(sameOrigin(URI("https://a"),URI("http://a")))
    }
    @Test fun reverseProxyPrefixSurvivesNegotiation() {
        val p=JellyfinProvider();val a=account().copy(server="$base/jellyfin")
        assertEquals("$base/jellyfin/Videos/movie-1/stream",p.trustedUrl(a,"/Videos/movie-1/stream"))
    }
    @Test fun transportDoesNotFollowCredentialRedirects() {
        assertThrows(ProtocolFailure::class.java) {Transport().bytes("$base/redirect",mapOf("Authorization" to "secret"))}
        assertFalse(requests.any{it.first=="/captured"})
    }
    @Test fun responseLimitIsEnforced() {assertThrows(ProtocolFailure::class.java){Transport().bytes(base,emptyMap(),limit=10)}}
    @Test fun snapshotKeyIsOpaqueAndFixedLength() {val k=Snapshots.key("private-account","private-film");assertEquals(64,k.length);assertFalse(k.contains("private"))}
    @Test fun durationFormatterMatchesUpstreamBoundary() {assertEquals("0:00",formatMillis(0));assertEquals("1:00:01",formatMillis(3601000));assertEquals("59:59",formatMillis(3599000))}
}
