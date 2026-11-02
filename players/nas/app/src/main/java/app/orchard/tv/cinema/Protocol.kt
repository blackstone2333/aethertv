// SPDX-License-Identifier: GPL-2.0-only
package app.orchard.tv.cinema

import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URI
import java.net.URLEncoder
import java.util.UUID

fun obj(vararg pairs: Pair<String, Any?>) = JSONObject().apply { pairs.forEach { (k, v) -> put(k, v ?: JSONObject.NULL) } }
fun JSONArray.objects(): List<JSONObject> = (0 until length()).mapNotNull { optJSONObject(it) }
fun JSONObject.array(key: String) = optJSONArray(key) ?: JSONArray()
fun JSONObject.text(key: String) = optString(key).takeUnless { it == "null" }.orEmpty()
fun enc(s: String): String = URLEncoder.encode(s, "UTF-8")
fun query(values: Map<String, Any>) = values.entries.joinToString("&") { "${enc(it.key)}=${enc(it.value.toString())}" }

// Server roots may include a reverse proxy path; never infer or remove it.
fun normalizeServer(raw: String): String {
    val uri = URI(raw.trim())
    require(uri.scheme in listOf("https", "http") && !uri.host.isNullOrBlank())
    require(uri.userInfo == null && uri.rawQuery == null && uri.rawFragment == null)
    return uri.toASCIIString().trimEnd('/')
}
fun sameOrigin(a: URI, b: URI): Boolean = a.scheme.equals(b.scheme, true) && a.host.equals(b.host, true) &&
    (if (a.port < 0) if (a.scheme == "https") 443 else 80 else a.port) ==
    (if (b.port < 0) if (b.scheme == "https") 443 else 80 else b.port)

enum class Backend { JELLYFIN, EMBY, MOONTV }
data class Account(val id: String, val backend: Backend, val server: String, val userId: String, val name: String, val token: String, val device: String, val version: String)
data class Media(val id: String, val title: String, val type: String, val overview: String, val year: Int, val ticks: Long, val resume: Long, val imageTag: String, val played: Boolean = false) {
    val folder get() = type in listOf("CollectionFolder", "Folder", "Series", "Season", "BoxSet", "UserView")
    val progress get() = if (ticks > 0) (resume.toFloat() / ticks).coerceIn(0f, 1f) else 0f
    companion object {
        fun parse(o: JSONObject): Media {
            val u = o.optJSONObject("UserData") ?: JSONObject()
            return Media(o.getString("Id"), o.text("Name").take(300), o.text("Type"), o.text("Overview").take(4000), o.optInt("ProductionYear"), o.optLong("RunTimeTicks"), u.optLong("PlaybackPositionTicks"), o.optJSONObject("ImageTags")?.text("Primary").orEmpty(), u.optBoolean("Played"))
        }
    }
}
data class Page(val items: List<Media>, val total: Int)
data class ServerTrack(val type: String, val index: Int, val label: String, val language: String)
data class Subtitle(val index: Int, val label: String, val language: String, val url: String)
data class Playback(val item: Media, val sourceId: String, val session: String, val url: String, val method: String, val startTicks: Long, val subtitles: List<Subtitle>, val audio: Int?, val subtitle: Int?, val serverTracks: List<ServerTrack> = emptyList()) {
    val offsetMillis get() = if (method == "DirectPlay") 0L else startTicks / 10_000
}
class ProtocolFailure(val reason: String, val status: Int = 0): Exception(reason)

class Transport {
    fun bytes(url: String, headers: Map<String, String>, limit: Int = 4 * 1024 * 1024, body: JSONObject? = null, method: String = if (body == null) "GET" else "POST", onHeaders: ((Map<String, List<String>>) -> Unit)? = null): ByteArray {
        val c = URI(url).toURL().openConnection() as HttpURLConnection
        try {
            c.instanceFollowRedirects = false
            c.connectTimeout = 8000
            c.readTimeout = 12000
            c.requestMethod = method
            headers.forEach { (k,v) -> c.setRequestProperty(k,v) }
            c.setRequestProperty("Accept", "application/json")
            if (body != null) {
                c.doOutput = true
                c.setRequestProperty("Content-Type", "application/json")
                c.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
            }
            val code = c.responseCode
            onHeaders?.invoke(c.headerFields.filterKeys { it != null })
            if (code !in 200..299) throw ProtocolFailure(if (code == 401 || code == 403) "auth" else "http", code)
            if (c.contentLength > limit) throw ProtocolFailure("limit")
            return c.inputStream.use { input ->
                val out = java.io.ByteArrayOutputStream()
                val b = ByteArray(8192)
                while (true) { val n = input.read(b); if (n < 0) break; if (out.size() + n > limit) throw ProtocolFailure("limit"); out.write(b,0,n) }
                out.toByteArray()
            }
        } finally { c.disconnect() }
    }
    fun json(url: String, headers: Map<String,String>, body: JSONObject? = null): JSONObject {
        val bytes = bytes(url, headers, body = body)
        return if (bytes.isEmpty()) JSONObject() else JSONObject(String(bytes, Charsets.UTF_8))
    }
}

interface MediaProvider {
    val backend: Backend
    fun login(server: String, username: String, password: String, device: String): Account
    fun libraries(a: Account): List<Media>
    fun items(a: Account, parent: String? = null, search: String? = null, start: Int = 0): Page
    fun resume(a: Account): List<Media>
    fun detail(a: Account, id: String): Media
    fun image(a: Account, m: Media): ByteArray
    fun negotiate(a: Account, m: Media, startTicks: Long, audio: Int? = null, subtitle: Int? = null, transcode: Boolean = false): Playback
    fun report(a: Account, p: Playback, event: String, ticks: Long, paused: Boolean, failed: Boolean = false)
}

abstract class BaseProvider(protected val http: Transport = Transport()): MediaProvider {
    protected abstract fun authHeader(token: String, device: String): Map<String,String>
    protected open fun root(server: String) = server
    protected open fun loginData(username: String, password: String) = obj("Username" to username, "Pw" to password)
    protected fun url(a: Account, path: String, params: Map<String,Any> = emptyMap()): String = root(a.server) + "/" + path.trimStart('/') + if (params.isEmpty()) "" else "?" + query(params)
    protected fun get(a: Account, path: String, params: Map<String,Any> = emptyMap()) = http.json(url(a,path,params), authHeader(a.token,a.device))
    override fun login(server: String, username: String, password: String, device: String): Account {
        val base = normalizeServer(server)
        val info = http.json(root(base) + "/System/Info/Public", authHeader("",device))
        validateServer(info)
        val data = http.json(root(base) + "/Users/AuthenticateByName", authHeader("",device), loginData(username,password))
        val user = data.getJSONObject("User")
        val token = data.getString("AccessToken")
        require(token.isNotEmpty())
        return Account(UUID.randomUUID().toString(), backend, base, user.getString("Id"), user.text("Name"), token, device, info.text("Version"))
    }
    protected abstract fun validateServer(info: JSONObject)
    override fun libraries(a: Account) = get(a,"Users/${enc(a.userId)}/Views").array("Items").objects().map(Media::parse)
    override fun items(a: Account, parent: String?, search: String?, start: Int): Page {
        val params = mutableMapOf<String,Any>("Limit" to 48, "StartIndex" to start, "Fields" to "Overview,MediaSources,MediaStreams", "EnableImageTypes" to "Primary", "SortBy" to "SortName", "SortOrder" to "Ascending")
        if (parent != null) params["ParentId"] = parent
        if (search != null) { params["SearchTerm"] = search; params["Recursive"] = true; params["IncludeItemTypes"] = "Movie,Series,Episode" }
        else if (parent == null) { params["Recursive"] = true; params["IncludeItemTypes"] = "Movie,Series" }
        val o = get(a, "Users/${enc(a.userId)}/Items", params)
        return Page(o.array("Items").objects().map(Media::parse),o.optInt("TotalRecordCount"))
    }
    override fun resume(a: Account) = get(a,"Users/${enc(a.userId)}/Items/Resume",mapOf("Limit" to 24,"MediaTypes" to "Video","Fields" to "Overview")).array("Items").objects().map(Media::parse)
    override fun detail(a: Account,id: String) = Media.parse(get(a,"Users/${enc(a.userId)}/Items/${enc(id)}"))
    override fun image(a: Account,m: Media) = http.bytes(url(a,"Items/${enc(m.id)}/Images/Primary",mapOf("maxWidth" to 320,"quality" to 80,"tag" to m.imageTag)),authHeader(a.token,a.device),512*1024)

    // Ported from Jellyfin Android TV v0.19.10 JellyfinMediaStreamResolver.
    // Keep the direct play -> remux -> transcode order; do not play filesystem paths.
    override fun negotiate(a: Account,m: Media,startTicks: Long,audio: Int?,subtitle: Int?,transcode: Boolean): Playback {
        val body = obj("UserId" to a.userId, "StartTimeTicks" to startTicks.coerceAtLeast(0), "IsPlayback" to true,
            "EnableDirectPlay" to !transcode, "EnableDirectStream" to !transcode, "EnableTranscoding" to true,
            "AllowVideoStreamCopy" to !transcode, "AllowAudioStreamCopy" to !transcode,
            "AutoOpenLiveStream" to false, "MaxStreamingBitrate" to 20_000_000, "DeviceProfile" to deviceProfile())
        if (audio != null) body.put("AudioStreamIndex",audio)
        body.put("SubtitleStreamIndex",subtitle ?: -1)
        val response = http.json(url(a,"Items/${enc(m.id)}/PlaybackInfo",mapOf("UserId" to a.userId)),authHeader(a.token,a.device),body)
        if (response.text("ErrorCode").isNotEmpty()) throw ProtocolFailure("media")
        val sources = response.array("MediaSources").objects().filter { !it.optBoolean("RequiresOpening") }
        val source = sources.firstOrNull { it.optBoolean("SupportsDirectPlay") && !transcode } ?: sources.firstOrNull { it.text("TranscodingUrl").isNotEmpty() } ?: throw ProtocolFailure("media")
        val sid = source.getString("Id")
        val session = response.text("PlaySessionId")
        val direct = source.optBoolean("SupportsDirectPlay") && !transcode
        val method = if (direct) "DirectPlay" else if (source.optBoolean("SupportsDirectStream") && !transcode) "DirectStream" else "Transcode"
        val stream = if (direct) url(a,"Videos/${enc(m.id)}/stream",mapOf("Static" to true,"MediaSourceId" to sid,"DeviceId" to a.device,"PlaySessionId" to session,"api_key" to a.token)) else trustedUrl(a,source.getString("TranscodingUrl"))
        val subs = source.array("MediaStreams").objects().filter { it.text("Type") == "Subtitle" && it.optBoolean("IsExternal") && it.text("Codec").lowercase() in listOf("srt","subrip","vtt","webvtt","ass","ssa") }.map {
            val index = it.getInt("Index")
            Subtitle(index,it.text("DisplayTitle").ifEmpty { it.text("Language") },it.text("Language"),url(a,"Videos/${enc(m.id)}/${enc(sid)}/Subtitles/$index/Stream.vtt",mapOf("api_key" to a.token)))
        }
        return Playback(m,sid,session,stream,method,startTicks,subs,audio,subtitle,source.array("MediaStreams").objects().filter { it.text("Type") in listOf("Audio","Subtitle") }.map { ServerTrack(it.text("Type"),it.getInt("Index"),it.text("DisplayTitle").ifEmpty {it.text("Language").ifEmpty{it.text("Codec")}},it.text("Language")) })
    }
    internal fun trustedUrl(a: Account, path: String): String {
        val base = URI(root(a.server) + "/")
        val candidate = URI(path)
        val result = if (candidate.isAbsolute) candidate else if (path.startsWith('/')) {
            // Jellyfin emits /Videos/... even behind a configured reverse proxy prefix.
            if (base.path != "/" && !path.startsWith(base.path)) URI(root(a.server) + path) else base.resolve(candidate)
        } else base.resolve(candidate)
        if (!sameOrigin(base,result) || result.userInfo != null) throw ProtocolFailure("origin")
        return result.toASCIIString()
    }
    // Ported from PlaySessionService: a single session emits start, periodic update and stop.
    override fun report(a: Account,p: Playback,event: String,ticks: Long,paused: Boolean,failed: Boolean) {
        require(event in listOf("Playing","Playing/Progress","Playing/Stopped"))
        val body = obj("ItemId" to p.item.id,"MediaSourceId" to p.sourceId,"PlaySessionId" to p.session,
            "PositionTicks" to ticks.coerceAtLeast(0),"IsPaused" to paused,"CanSeek" to true,"PlayMethod" to p.method,
            "Failed" to failed,"RepeatMode" to "RepeatNone")
        if (p.audio != null) body.put("AudioStreamIndex",p.audio)
        body.put("SubtitleStreamIndex",p.subtitle ?: -1)
        http.json(url(a,"Sessions/$event"),authHeader(a.token,a.device),body)
        if (event == "Playing/Stopped" && p.method != "DirectPlay") {
            http.bytes(url(a,"Videos/ActiveEncodings",mapOf("DeviceId" to a.device,"PlaySessionId" to p.session)),authHeader(a.token,a.device),method="DELETE")
        }
    }
}
class JellyfinProvider: BaseProvider() {
    override val backend = Backend.JELLYFIN
    override fun authHeader(token: String,device: String) = mapOf("Authorization" to "MediaBrowser Client=\"AetherCinema\", Device=\"Android TV\", DeviceId=\"$device\", Version=\"0.1.0\"" + if (token.isEmpty()) "" else ", Token=\"$token\"")
    override fun validateServer(info: JSONObject) { if (!info.text("ProductName").contains("Jellyfin",true)) throw ProtocolFailure("backend") }
}
class EmbyProvider: BaseProvider() {
    override val backend = Backend.EMBY
    override fun root(server: String) = if (URI(server).path.trimEnd('/').endsWith("/emby",true)) server else "$server/emby"
    override fun authHeader(token: String,device: String) = mapOf("X-Emby-Authorization" to "MediaBrowser Client=\"AetherCinema\", Device=\"Android TV\", DeviceId=\"$device\", Version=\"0.1.0\"", "X-Emby-Token" to token)
    override fun loginData(username: String,password: String): JSONObject {
        val sha = java.security.MessageDigest.getInstance("SHA-1").digest(password.toByteArray(Charsets.UTF_8)).joinToString("") { "%02x".format(it) }
        return obj("Username" to username,"Pw" to password,"Password" to sha)
    }
    override fun validateServer(info: JSONObject) { if (info.text("ProductName").contains("Jellyfin",true) || info.text("Version").isEmpty()) throw ProtocolFailure("backend") }
}
fun provider(backend: Backend): MediaProvider = when (backend) { Backend.JELLYFIN -> JellyfinProvider(); Backend.EMBY -> EmbyProvider(); Backend.MOONTV -> MoonTvProvider() }

// Conservative Android 6 profile: hardware AVC/AAC first; server converts other formats.
// Derived from upstream deviceProfile.kt; no unverified HEVC/DTS claims.
fun deviceProfile(): JSONObject = obj("Name" to "AetherCinema Android 6", "MaxStreamingBitrate" to 20_000_000,
    "MaxStaticBitrate" to 40_000_000, "MusicStreamingTranscodingBitrate" to 192000,
    "DirectPlayProfiles" to JSONArray().put(obj("Type" to "Video","Container" to "mp4,m4v,mov,mkv,webm","VideoCodec" to "h264","AudioCodec" to "aac,mp3")),
    "TranscodingProfiles" to JSONArray().put(obj("Type" to "Video","Container" to "ts","Protocol" to "hls","VideoCodec" to "h264","AudioCodec" to "aac","Context" to "Streaming","MaxAudioChannels" to "2","MinSegments" to 2,"BreakOnNonKeyFrames" to true)),
    "CodecProfiles" to JSONArray().put(obj("Type" to "Video","Codec" to "h264","Conditions" to JSONArray().put(obj("Condition" to "LessThanEqual","Property" to "VideoLevel","Value" to "41","IsRequired" to true)).put(obj("Condition" to "LessThanEqual","Property" to "Width","Value" to "1920","IsRequired" to true)).put(obj("Condition" to "LessThanEqual","Property" to "Height","Value" to "1080","IsRequired" to true)))),
    "SubtitleProfiles" to JSONArray().put(obj("Format" to "vtt","Method" to "External")).put(obj("Format" to "srt","Method" to "External")).put(obj("Format" to "ass","Method" to "Encode")))
