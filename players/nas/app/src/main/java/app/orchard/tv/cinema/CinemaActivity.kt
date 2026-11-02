// SPDX-License-Identifier: GPL-2.0-only
package app.orchard.tv.cinema

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.BackHandler
import androidx.activity.viewModels
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.platform.*
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.*
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import app.orchard.tv.R
import app.orchard.tv.ui.components.Pressable
import app.orchard.tv.ui.theme.*

open class CinemaActivity: ComponentActivity() {
    private val model: CinemaModel by viewModels()
    override fun onCreate(state: Bundle?) {
        super.onCreate(state)
        window.decorView.systemUiVisibility = android.view.View.SYSTEM_UI_FLAG_FULLSCREEN or android.view.View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or android.view.View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        consume(intent)
        setContent {
            val metrics=resources.displayMetrics
            val density=minOf(metrics.widthPixels,metrics.heightPixels*16/9)/960f
            val reduced=Settings.Global.getFloat(contentResolver,Settings.Global.ANIMATOR_DURATION_SCALE,1f)==0f
            CompositionLocalProvider(LocalDensity provides Density(density,1f),LocalReducedMotion provides reduced) { Cinema(model) }
        }
    }
    override fun onNewIntent(intent: Intent) { super.onNewIntent(intent); setIntent(intent); consume(intent) }
    private fun consume(i: Intent) {
        if (this !is PlayItemActivity) return
        if (i.action == "app.aethertv.cinema.PLAY_ITEM" && i.getIntExtra("contract_version",0)==1) {
            // External calls carry opaque IDs only. Resolution and authentication stay here.
            model.launchItem(i.getStringExtra("account_id"),i.getStringExtra("item_id"),if (i.hasExtra("position_ticks")) i.getLongExtra("position_ticks",0) else null)
        }
    }
    override fun onStart() {super.onStart();model.foreground=true}
    override fun onStop() {model.foreground=false;model.artwork.clear();super.onStop()}
    override fun onPause() { model.stopPlayer?.invoke(); model.playback=null; super.onPause() }
}

@Composable fun Label(text: String,modifier: Modifier=Modifier,style: TextStyle=Type.Body,maxLines: Int=Int.MAX_VALUE) = BasicText(text,modifier,style,maxLines=maxLines,overflow=TextOverflow.Ellipsis)
@Composable fun Cinema(model: CinemaModel) {
    BackHandler(model.playback!=null || model.selected!=null || model.pages.size>1 || (model.accountPage && model.account!=null)) { model.back() }
    Box(Modifier.fillMaxSize().background(Brush.linearGradient(listOf(Color(0xFF1C2537),Palette.Background,Color(0xFF080B12))))) {
        when {
            !model.foreground -> Unit
            model.playback!=null -> PlayerScreen(model,model.playback!!)
            model.accountPage -> Accounts(model)
            model.selected!=null -> Detail(model,model.selected!!)
            else -> Library(model)
        }
        if (model.busy) Box(Modifier.align(Alignment.BottomEnd).padding(28.dp).clip(RoundedCornerShape(20.dp)).background(Palette.Glass).padding(16.dp,10.dp)) { Label(stringResource(R.string.loading)) }
        model.error?.let { reason ->
            Column(Modifier.align(Alignment.BottomCenter).padding(24.dp).widthIn(max=650.dp).clip(RoundedCornerShape(18.dp)).background(Color(0xF52C2428)).padding(16.dp)) {
                Label(stringResource(when(reason) { "auth" -> R.string.error_auth; "address" -> R.string.error_address; "backend" -> R.string.error_backend; "media" -> R.string.error_media; else -> R.string.error_network }))
                Row(horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                    Action(stringResource(R.string.dismiss),{ model.error=null })
                    if (reason=="auth") Action(stringResource(R.string.accounts),{ model.error=null; model.accountPage=true })
                }
            }
        }
    }
}
@Composable fun Action(text: String,onClick: () -> Unit,modifier: Modifier=Modifier,requester: FocusRequester?=null,enabled: Boolean=true) {
    Pressable(onClick,modifier,enabled=enabled,focusRequester=requester) { state ->
        Box(Modifier.clip(RoundedCornerShape(16.dp)).background(if(state.focused) Palette.Focus else Palette.Glass)
            .border(1.dp,if(state.focused) Color.White else Color.White.copy(alpha=.10f),RoundedCornerShape(16.dp)).padding(18.dp,11.dp)) {
            Label(text,style=Type.Body.copy(color=if(state.focused) Palette.OnFocus else if(enabled) Palette.Label else Palette.SecondaryLabel,fontWeight=FontWeight.Medium))
        }
    }
}
@Composable private fun Header(model: CinemaModel,title: String,extra: @Composable RowScope.() -> Unit = {}) {
    Row(Modifier.fillMaxWidth().padding(bottom=20.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(14.dp)) {
        Column(Modifier.weight(1f)) { Label(stringResource(R.string.app_name),style=Type.Caption); Label(title,style=Type.Title) }
        extra()
        Action(stringResource(R.string.accounts),{ model.accountPage=true })
    }
}
@Composable private fun Library(model: CinemaModel) {
    val page=model.pages.lastOrNull()
    var searching by remember { mutableStateOf(false) }
    var term by remember { mutableStateOf("") }
    Column(Modifier.fillMaxSize().padding(38.dp,24.dp)) {
        Header(model,page?.title?.takeIf { it.isNotBlank() } ?: stringResource(R.string.library)) { Action(stringResource(R.string.search),{ searching=!searching }) }
        if (searching) Row(Modifier.fillMaxWidth().padding(bottom=15.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)) {
            Field(stringResource(R.string.search_hint),term,{term=it},Modifier.weight(1f))
            Action(stringResource(R.string.search),{ model.search(term); searching=false })
        }
        if (model.pages.size<=1) {
            LazyRow(horizontalArrangement=Arrangement.spacedBy(14.dp),contentPadding=PaddingValues(4.dp),modifier=Modifier.padding(bottom=14.dp)) {
                item { Action(stringResource(R.string.refresh),{model.home()}) }
                items(model.libraries,key={it.id}) { m -> Action(if(m.id=="moon:favorites") stringResource(R.string.favorites) else m.title,{model.browse(m)}) }
            }
            if (model.continuing.isNotEmpty()) {
                Label(stringResource(R.string.continue_watching),style=Type.Body.copy(fontWeight=FontWeight.SemiBold),modifier=Modifier.padding(bottom=8.dp))
                LazyRow(horizontalArrangement=Arrangement.spacedBy(18.dp),contentPadding=PaddingValues(7.dp),modifier=Modifier.height(104.dp)) {
                    items(model.continuing,key={it.id}) { m -> ResumeCard(model,m) }
                }
            }
        }
        if (page!=null) key(page.key) { PosterGrid(model,page,Modifier.weight(1f)) }
        else if (!model.busy) Label(stringResource(R.string.empty_library))
    }
}
@Composable private fun PosterGrid(model: CinemaModel,page: BrowsePage,modifier: Modifier) {
    val state=rememberLazyGridState(page.scrollIndex,page.scrollOffset)
    val focus=remember { FocusRequester() }
    val target=page.focusId ?: page.items.firstOrNull()?.id
    DisposableEffect(page) { onDispose { page.scrollIndex=state.firstVisibleItemIndex; page.scrollOffset=state.firstVisibleItemScrollOffset } }
    LaunchedEffect(page.items.isNotEmpty()) { if (page.items.isNotEmpty()) { kotlinx.coroutines.delay(80); runCatching {focus.requestFocus()} } }
    LazyVerticalGrid(GridCells.Fixed(6),modifier, state=state,contentPadding=PaddingValues(8.dp),verticalArrangement=Arrangement.spacedBy(22.dp),horizontalArrangement=Arrangement.spacedBy(18.dp)) {
        items(page.items,key={it.id}) { m ->
            Poster(model,m,if(m.id==target) focus else null,{page.focusId=m.id})
        }
        if(page.items.size<page.total && page.items.size<480) item(span={GridItemSpan(6)}) { Action(stringResource(R.string.load_more),{model.more()}) }
        if(page.items.isEmpty() && !model.busy) item(span={GridItemSpan(6)}) { Label(stringResource(R.string.empty_library)) }
    }
}
@Composable private fun Art(model: CinemaModel,m: Media,modifier: Modifier) {
    val a=model.account
    val bitmap by produceState<android.graphics.Bitmap?>(null,a?.id,m.id,m.imageTag) { if(a!=null && m.imageTag.isNotEmpty()) value=model.artwork.load(a,m) }
    Box(modifier.background(Brush.linearGradient(listOf(Color(0xFF303B50),Color(0xFF151B29)))),contentAlignment=Alignment.Center) {
        if(bitmap!=null) Image(bitmap!!.asImageBitmap(),m.title,Modifier.fillMaxSize(),contentScale=androidx.compose.ui.layout.ContentScale.Crop)
        else Label(m.title.take(1),style=Type.Title.copy(color=Palette.SecondaryLabel))
    }
}
@Composable private fun Poster(model: CinemaModel,m: Media,requester: FocusRequester?,onFocus: () -> Unit) {
    Pressable({model.open(m)},Modifier.onFocusChanged {if(it.isFocused) onFocus()},focusRequester=requester,focusedScale=1.055f) { state ->
        Column {
            Box(Modifier.fillMaxWidth().aspectRatio(2f/3).border(if(state.focused) 3.dp else 1.dp,if(state.focused) Color.White else Color.White.copy(alpha=.08f),RoundedCornerShape(12.dp)).padding(if(state.focused) 3.dp else 1.dp).clip(RoundedCornerShape(10.dp))) {
                Art(model,m,Modifier.fillMaxSize())
                if(m.progress>0f) Box(Modifier.align(Alignment.BottomStart).fillMaxWidth(m.progress).height(4.dp).background(Color.White))
            }
            Label(m.title,Modifier.padding(top=7.dp),Type.Body.copy(fontSize=13.sp,fontWeight=if(state.focused) FontWeight.SemiBold else FontWeight.Normal),maxLines=1)
            Label(if(m.year>0) m.year.toString() else "",style=Type.Caption)
        }
    }
}
@Composable private fun ResumeCard(model: CinemaModel,m: Media) {
    Pressable({model.open(m)},focusedScale=1.04f) { state ->
        Row(Modifier.width(258.dp).height(85.dp).clip(RoundedCornerShape(14.dp)).background(if(state.focused) Palette.Focus else Palette.Glass).padding(7.dp),verticalAlignment=Alignment.CenterVertically) {
            Art(model,m,Modifier.width(48.dp).fillMaxHeight().clip(RoundedCornerShape(7.dp)))
            Column(Modifier.padding(start=10.dp).weight(1f),verticalArrangement=Arrangement.spacedBy(7.dp)) {
                Label(m.title,style=Type.Body.copy(fontSize=14.sp,color=if(state.focused) Palette.OnFocus else Palette.Label),maxLines=1)
                Box(Modifier.fillMaxWidth().height(3.dp).background(Color.Gray)) { Box(Modifier.fillMaxWidth(m.progress).fillMaxHeight().background(if(state.focused) Color(0xFF3B5276) else Color.White)) }
                Label(formatMillis(m.resume/10000),style=Type.Caption.copy(color=if(state.focused) Palette.OnFocus else Palette.SecondaryLabel))
            }
        }
    }
}
@Composable private fun Detail(model: CinemaModel,m: Media) {
    val play=remember { FocusRequester() }
    LaunchedEffect(m.id) { play.requestFocus() }
    Row(Modifier.fillMaxSize().padding(48.dp,40.dp),horizontalArrangement=Arrangement.spacedBy(38.dp)) {
        Art(model,m,Modifier.width(242.dp).fillMaxHeight().clip(RoundedCornerShape(18.dp)))
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(16.dp)) {
            Label(stringResource(R.string.library),style=Type.Caption)
            Label(m.title,style=Type.Title.copy(fontSize=38.sp,lineHeight=44.sp))
            Label(listOfNotNull(m.year.takeIf {it>0}?.toString(),m.ticks.takeIf{it>0}?.let {formatMillis(it/10000)}).joinToString("   ·   "),style=Type.Caption)
            Row(horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                Action(if(m.resume>0) stringResource(R.string.resume_at,formatMillis(m.resume/10000)) else stringResource(R.string.play),{model.play()},requester=play)
                if(m.resume>0) Action(stringResource(R.string.from_start),{model.play(true)})
            }
            Label(m.overview.ifBlank {stringResource(R.string.no_overview)},style=Type.Body.copy(color=Palette.SecondaryLabel),maxLines=8)
            if(model.account?.backend!=Backend.MOONTV) Action(stringResource(R.string.compatibility_play),{model.play(transcode=true)})
        }
    }
}
@Composable private fun Accounts(model: CinemaModel) {
    var server by remember { mutableStateOf("") }; var user by remember { mutableStateOf("") }; var password by remember { mutableStateOf("") }
    var backend by remember { mutableStateOf(Backend.JELLYFIN) }
    val first=remember { FocusRequester() }
    LaunchedEffect(Unit) {first.requestFocus()}
    Row(Modifier.fillMaxSize().padding(42.dp,30.dp),horizontalArrangement=Arrangement.spacedBy(30.dp)) {
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(16.dp)) {
            Label(stringResource(R.string.app_name),style=Type.Title)
            Label(stringResource(R.string.account_intro),style=Type.Body.copy(color=Palette.SecondaryLabel))
            model.accounts.forEach { a ->
                Row(horizontalArrangement=Arrangement.spacedBy(10.dp)) {
                    Action("${a.name} · ${stringResource(when(a.backend) { Backend.JELLYFIN -> R.string.jellyfin; Backend.EMBY -> R.string.emby; Backend.MOONTV -> R.string.moontv })}",{model.use(a)})
                    Action(stringResource(R.string.remove_account),{model.remove(a)})
                }
            }
            Label(stringResource(R.string.providers_pending),style=Type.Caption)
            Label(stringResource(R.string.license_notice),style=Type.Caption)
        }
        Column(Modifier.width(470.dp).clip(RoundedCornerShape(24.dp)).background(Palette.Glass).padding(24.dp).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(13.dp)) {
            Label(stringResource(R.string.add_account),style=Type.Body.copy(fontWeight=FontWeight.SemiBold))
            Row(horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                Action((if(backend==Backend.JELLYFIN) "✓ " else "")+stringResource(R.string.jellyfin),{backend=Backend.JELLYFIN},requester=first)
                Action((if(backend==Backend.EMBY) "✓ " else "")+stringResource(R.string.emby),{backend=Backend.EMBY})
                Action((if(backend==Backend.MOONTV) "✓ " else "")+stringResource(R.string.moontv),{backend=Backend.MOONTV})
            }
            Field(stringResource(R.string.server),server,{server=it},keyboard=KeyboardType.Uri)
            Field(stringResource(R.string.username),user,{user=it})
            Field(stringResource(R.string.password),password,{password=it},password=true)
            Action(stringResource(R.string.sign_in),{model.login(server,user,password,backend); password=""},enabled=!model.busy && server.isNotBlank() && user.isNotBlank())
            Label(stringResource(R.string.account_privacy),style=Type.Caption)
        }
    }
}
@Composable private fun Field(title: String,value: String,onChange: (String) -> Unit,modifier: Modifier=Modifier,password: Boolean=false,keyboard: KeyboardType=KeyboardType.Text) {
    var focused by remember { mutableStateOf(false) }
    Column(modifier,verticalArrangement=Arrangement.spacedBy(6.dp)) {
        Label(title,style=Type.Caption)
        BasicTextField(value,{onChange(it.take(2048))},Modifier.fillMaxWidth().onFocusChanged{focused=it.isFocused}.border(1.dp,if(focused) Color.White else Color.White.copy(alpha=.15f),RoundedCornerShape(10.dp)).background(Color(0xFF121722),RoundedCornerShape(10.dp)).padding(12.dp),textStyle=Type.Body,singleLine=true,visualTransformation=if(password) PasswordVisualTransformation() else VisualTransformation.None,keyboardOptions=KeyboardOptions(keyboardType=if(password) KeyboardType.Password else keyboard),cursorBrush=SolidColor(Color.White))
    }
}
// Ported from upstream TimeUtils.formatMillis, locale-stable for a TV progress display.
fun formatMillis(raw: Long): String {
    val seconds=raw.coerceAtLeast(0)/1000
    return if(seconds>=3600) "%d:%02d:%02d".format(java.util.Locale.ROOT,seconds/3600,seconds/60%60,seconds%60) else "%d:%02d".format(java.util.Locale.ROOT,seconds/60,seconds%60)
}

class PlayItemActivity: CinemaActivity()
