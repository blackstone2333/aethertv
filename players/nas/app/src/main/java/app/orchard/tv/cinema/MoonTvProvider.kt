// SPDX-License-Identifier: GPL-2.0-only
package app.orchard.tv.cinema

import org.json.JSONObject
import java.net.URI
import android.util.Base64

/** Deployed MoonTV protocol (2026-10-28), independently implemented from observed HTTP/JS.
 * Source repository supplied by user currently returns 404. No web JavaScript is bundled.
 * Cookie authentication is never sent to media CDNs. No admin or source-management calls.
 */
class MoonTvProvider(private val http: Transport = Transport()): MediaProvider {
    override val backend=Backend.MOONTV
    private fun headers(a: Account)=mapOf("Cookie" to a.token)
    private fun api(a: Account,path: String,body: JSONObject?=null)=http.json(a.server+path,headers(a),body)
    override fun login(server: String,username: String,password: String,device: String): Account {
        val base=normalizeServer(server)
        var cookie=""
        http.bytes(base+"/api/login",emptyMap(),body=obj("username" to username,"password" to password),onHeaders={ h ->
            cookie=h.entries.filter{it.key.equals("Set-Cookie",true)}.flatMap{it.value}.map{it.substringBefore(';')}.filter{it.contains('=')}.joinToString("; ")
        })
        if(cookie.isEmpty()) throw ProtocolFailure("auth")
        val a=Account(java.util.UUID.randomUUID().toString(),backend,base,username,username,cookie,device,"deployed-20261028")
        api(a,"/api/playrecords")
        return a
    }
    override fun libraries(a: Account)=listOf(Media("moon:favorites","", "CollectionFolder","",0,0,0,""))
    private fun decode(id: String): JSONObject {
        require(id.length<=4096)
        return JSONObject(String(Base64.decode(id,Base64.URL_SAFE or Base64.NO_WRAP),Charsets.UTF_8)).apply {
            require(text("s").isNotEmpty() && text("i").isNotEmpty())
        }
    }
    internal fun id(source: String,item: String,episode: Int?=null): String = Base64.encodeToString(obj("s" to source,"i" to item,"e" to episode).toString().toByteArray(),Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
    companion object {
        private val cache=object: LinkedHashMap<String,Pair<Long,JSONObject>>(16,.75f,true) {
            override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String,Pair<Long,JSONObject>>?) = size>16
        }
    }
    private fun details(a: Account,key: JSONObject): JSONObject {
        val id=a.id+":"+key.getString("s")+":"+key.getString("i")
        synchronized(cache) { cache[id]?.takeIf {System.currentTimeMillis()-it.first<300000}?.let{return it.second} }
        return api(a,"/api/detail?source=${enc(key.getString("s"))}&id=${enc(key.getString("i"))}").also { synchronized(cache) {cache[id]=System.currentTimeMillis() to it} }
    }
    private fun parse(o: JSONObject,episode: Int?=null,resume: Long=0,total: Long=0): Media {
        val ep=episode?.let{ idx -> o.array("episodes_titles").optString(idx).takeIf{it.isNotBlank()} ?: (idx+1).toString() }
        return Media(id(o.text("source"),o.text("id"),episode),o.text("title")+(ep?.let{" · $it"} ?: o.text("source_name").takeIf{it.isNotBlank()}?.let{" · $it"}.orEmpty()),if(episode==null) "Series" else "Episode",
            o.text("desc").replace(Regex("<[^>]*>"),"").take(4000),o.text("year").toIntOrNull() ?: 0,total,resume,o.text("poster"))
    }
    override fun items(a: Account,parent: String?,search: String?,start: Int): Page {
        if(search!=null) {
            val results=api(a,"/api/search?q=${enc(search.take(128))}").array("results").objects()
            return Page(results.drop(start).take(48).map{parse(it)},results.size)
        }
        if(parent!=null && parent!="moon:favorites") {
            val d=details(a,decode(parent));val length=d.array("episodes").length()
            return Page((start until minOf(start+48,length)).map{parse(d,it)},length)
        }
        val fav=api(a,"/api/favorites")
        val keys=fav.keys().asSequence().toList()
        val list=keys.drop(start).take(48).mapNotNull { key ->
            val source=key.substringBefore('+');val item=key.substringAfter('+',"");if(item.isEmpty()) return@mapNotNull null
            val f=fav.optJSONObject(key) ?: return@mapNotNull null
            Media(id(source,item),f.text("title"),"Series","",f.text("year").toIntOrNull() ?: 0,0,0,f.text("cover"))
        }
        return Page(list,keys.size)
    }
    override fun resume(a: Account): List<Media> {
        val records=api(a,"/api/playrecords")
        return records.keys().asSequence().mapNotNull { key -> records.optJSONObject(key)?.let{key to it} }.sortedByDescending{it.second.optLong("save_time")}.take(24).mapNotNull { (key,r) ->
            val source=key.substringBefore('+');val item=key.substringAfter('+',"")
            if(item.isEmpty()) return@mapNotNull null
            Media(id(source,item,(r.optInt("index",1)-1).coerceAtLeast(0)),r.text("title"),"Episode","",r.text("year").toIntOrNull() ?: 0,r.optLong("total_time")*10_000_000,r.optLong("play_time")*10_000_000,r.text("cover"))
        }.toList()
    }
    override fun detail(a: Account,id: String): Media {
        val key=decode(id)
        val records=api(a,"/api/playrecords")
        val record=records.optJSONObject(key.getString("s")+"+"+key.getString("i"))
        val episode=if(key.isNull("e")) null else key.getInt("e")
        val same=record!=null && record.optInt("index",1)-1==episode
        return parse(details(a,key),episode,if(same) record!!.optLong("play_time")*10_000_000 else 0,if(same) record!!.optLong("total_time")*10_000_000 else 0)
    }
    override fun image(a: Account,m: Media): ByteArray {
        val u=URI(m.imageTag)
        require(u.scheme in listOf("http","https") && u.host!=null && u.userInfo==null)
        return http.bytes(u.toASCIIString(),if(sameOrigin(URI(a.server),u)) headers(a) else emptyMap(),512*1024)
    }
    override fun negotiate(a: Account,m: Media,startTicks: Long,audio: Int?,subtitle: Int?,transcode: Boolean): Playback {
        if(transcode) throw ProtocolFailure("media")
        val key=decode(m.id);val d=details(a,key);val index=key.optInt("e",0)
        val stream=d.array("episodes").optString(index)
        val u=URI(stream)
        if(u.scheme !in listOf("https","http") || u.host==null || u.userInfo!=null) throw ProtocolFailure("media")
        return Playback(m,key.getString("s"),key.getString("i"),u.toASCIIString(),"DirectPlay",startTicks,emptyList(),audio,subtitle)
    }
    override fun report(a: Account,p: Playback,event: String,ticks: Long,paused: Boolean,failed: Boolean) {
        if(event=="Playing" || ticks<10_000_000 || p.item.ticks<=0 || failed) return
        val key=decode(p.item.id);val d=details(a,key)
        val record=obj("title" to d.text("title"),"source_name" to d.text("source_name"),"year" to d.text("year"),"cover" to d.text("poster"),"index" to key.optInt("e",0)+1,"total_episodes" to d.array("episodes").length(),"play_time" to ticks/10_000_000,"total_time" to p.item.ticks/10_000_000,"save_time" to System.currentTimeMillis(),"search_title" to d.text("title"))
        api(a,"/api/playrecords",obj("key" to key.getString("s")+"+"+key.getString("i"),"record" to record))
    }
}
