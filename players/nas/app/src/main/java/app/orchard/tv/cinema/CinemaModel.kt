// SPDX-License-Identifier: GPL-2.0-only
package app.orchard.tv.cinema

import android.app.Application
import androidx.compose.runtime.*
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.*

class BrowsePage(val key: String,val title: String,val parent: String? = null,val search: String? = null) {
    var items by mutableStateOf<List<Media>>(emptyList())
    var total by mutableIntStateOf(0)
    var focusId: String? = null
    var scrollIndex = 0
    var scrollOffset = 0
}
class CinemaModel(app: Application): AndroidViewModel(app) {
    val store = AccountStore(app)
    val resumes = ResumeStore(app)
    val artwork = Artwork(app)
    var accounts by mutableStateOf(store.all()); private set
    var account by mutableStateOf(accounts.firstOrNull { it.id == store.active }); private set
    var accountPage by mutableStateOf(account == null)
    var pages = mutableStateListOf<BrowsePage>()
    var selected by mutableStateOf<Media?>(null)
    var libraries by mutableStateOf<List<Media>>(emptyList())
    var continuing by mutableStateOf<List<Media>>(emptyList())
    var playback by mutableStateOf<Playback?>(null)
    var foreground by mutableStateOf(true)
    var busy by mutableStateOf(false)
    var error by mutableStateOf<String?>(null)
    var stopPlayer: (() -> Unit)? = null
    private var task: Job? = null
    @Volatile private var generation = 0
    init { Snapshots.activate(getApplication(),account?.id); if (account != null) home() }
    fun operation(work: suspend () -> Unit) {
        task?.cancel()
        task = viewModelScope.launch {
            busy = true; error = null
            try { work() } catch (e: CancellationException) { throw e } catch (e: Exception) {
                error = when { e is ProtocolFailure && e.reason == "auth" -> "auth"; e is ProtocolFailure && e.reason == "backend" -> "backend"; e is ProtocolFailure && e.reason == "media" -> "media"; e is IllegalArgumentException -> "address"; else -> "network" }
            } finally { busy = false }
        }
    }
    fun login(server: String,user: String,password: String,backend: Backend) = operation {
        val a = withContext(Dispatchers.IO) { provider(backend).login(server,user,password,store.device).also { store.save(it) } }
        accounts = store.all(); account=a; Snapshots.activate(getApplication(),a.id); generation++; pages.clear(); selected=null; accountPage=false
        loadHome(a)
    }
    fun use(a: Account) { task?.cancel(); generation++; account=a; store.active=a.id; pages.clear(); selected=null; continuing=emptyList(); libraries=emptyList(); accountPage=false; Snapshots.activate(getApplication(),a.id); home() }
    fun remove(a: Account) {
        store.remove(a.id); resumes.clear(a); accounts=store.all()
        if (account?.id == a.id) { task?.cancel(); generation++; account=null; pages.clear(); selected=null; libraries=emptyList(); continuing=emptyList(); Snapshots.activate(getApplication(),null) }
    }
    fun home() { val a=account ?: return; operation { loadHome(a) } }
    private suspend fun loadHome(a: Account) {
        val p = provider(a.backend)
        val data = withContext(Dispatchers.IO) { Triple(p.libraries(a), p.resume(a), p.items(a)) }
        libraries=data.first; continuing=data.second.map { resumes.overlay(a,it) }
        if (pages.isEmpty()) pages.add(BrowsePage("home",""))
        pages[0].items=data.third.items.map { resumes.overlay(a,it) }; pages[0].total=data.third.total
        publish(a,continuing + pages[0].items)
    }
    private fun publish(a: Account,items: List<Media>) {
        val g=generation
        viewModelScope.launch(Dispatchers.IO) {
            if (g != generation) return@launch
            Snapshots.write(getApplication(),a,items)
            val dir=java.io.File(getApplication<Application>().filesDir,"snapshot-art").apply { mkdirs() }
            val keys=items.filterNot { it.folder }.distinctBy { it.id }.take(24).map { Snapshots.key(a.id,it.id) }.toSet()
            dir.listFiles()?.filterNot { it.nameWithoutExtension in keys }?.forEach { it.delete() }
            items.filterNot { it.folder }.distinctBy { it.id }.take(24).forEach {
                if (g != generation) return@launch
                val bitmap = artwork.load(a,it) ?: return@forEach
                if (g != generation) return@launch
                Snapshots.art(getApplication(),a.id,it.id,bitmap)
            }
        }
    }
    fun browse(m: Media) { val a=account ?: return; val page=BrowsePage("folder:${m.id}",m.title,m.id); pages.add(page); selected=null; operation { load(a,page) } }
    fun search(text: String) { if (text.isBlank()) return; val a=account ?: return; val page=BrowsePage("search:$text",text,search=text.take(128)); pages.add(page); operation { load(a,page) } }
    private suspend fun load(a: Account,page: BrowsePage,start: Int = 0) {
        val data=withContext(Dispatchers.IO) { provider(a.backend).items(a,page.parent,page.search,start) }
        page.items=if (start==0) data.items.map { resumes.overlay(a,it) } else (page.items+data.items.map { resumes.overlay(a,it) }).distinctBy { it.id }.take(480)
        page.total=data.total
    }
    fun more() { val a=account ?: return; val page=pages.lastOrNull() ?: return; operation { load(a,page,page.items.size) } }
    fun open(m: Media) { if (m.folder) { browse(m); return }; val a=account ?: return; operation { selected=withContext(Dispatchers.IO) { resumes.overlay(a,provider(a.backend).detail(a,m.id)) } } }
    fun play(fromStart: Boolean = false, transcode: Boolean = false) { val a=account ?: return; val m=selected ?: return; operation { playback=withContext(Dispatchers.IO) { provider(a.backend).negotiate(a,m,if (fromStart) 0 else m.resume,transcode=transcode) } } }
    fun changeServerTrack(p: Playback,ticks: Long,audio: Int?=p.audio,subtitle: Int?=p.subtitle) {
        val a=account ?: return
        operation {
            val next=withContext(Dispatchers.IO) {provider(a.backend).negotiate(a,p.item,ticks,audio,subtitle,transcode=true)}
            stopPlayer?.invoke()
            playback=next
        }
    }
    fun launchItem(accountId: String?,itemId: String?,position: Long?) {
        if (accountId == null || itemId.isNullOrBlank() || itemId.length>4096) return
        val a=accounts.firstOrNull { it.id==accountId } ?: return
        use(a)
        operation {
            val m=withContext(Dispatchers.IO) { resumes.overlay(a,provider(a.backend).detail(a,itemId)) }
            if (m.folder) return@operation
            selected=m; accountPage=false
            playback=withContext(Dispatchers.IO) { provider(a.backend).negotiate(a,m,position?.coerceIn(0,m.ticks.takeIf {it>0} ?: Long.MAX_VALUE) ?: m.resume) }
        }
    }
    fun back(): Boolean = when {
        playback!=null -> { stopPlayer?.invoke(); playback=null; true }
        selected!=null -> { selected=null; true }
        accountPage && account!=null -> { accountPage=false; true }
        pages.size>1 -> { task?.cancel(); busy=false; pages.removeAt(pages.lastIndex); true }
        else -> false
    }
    fun stopped(p: Playback,ticks: Long) {
        val a=account ?: return
        resumes.save(a,p.item.id,ticks)
        selected=selected?.copy(resume=ticks)
        pages.forEach { page -> page.items=page.items.map { if (it.id==p.item.id) it.copy(resume=ticks) else it } }
        continuing=(listOf(p.item.copy(resume=ticks))+continuing.filterNot { it.id==p.item.id }).filter { it.resume>0 && (it.ticks==0L || it.resume<it.ticks*0.95) }.take(24)
        publish(a,continuing+pages.firstOrNull()?.items.orEmpty())
    }
    override fun onCleared() { artwork.clear() }
}
