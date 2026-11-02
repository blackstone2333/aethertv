// SPDX-License-Identifier: GPL-2.0-only
package app.orchard.tv.cinema

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.os.ParcelFileDescriptor
import java.io.File

/** Signature protected, bounded, offline-only. No network from provider callbacks. */
class SnapshotProvider: ContentProvider() {
    override fun onCreate() = true
    override fun getType(uri: Uri): String? = when (uri.pathSegments.firstOrNull()) { "v1" -> "vnd.android.cursor.dir/vnd.aethercinema.card"; "art" -> "image/jpeg"; else -> null }
    override fun query(uri: Uri, projection: Array<out String>?, selection: String?, selectionArgs: Array<out String>?, sortOrder: String?): Cursor {
        require(uri.path == "/v1/cards")
        val c = MatrixCursor(arrayOf("version","account_id","item_id","title","art_uri","position_ticks","duration_ticks","updated_at"))
        val active=AccountStore(context!!).active
        val rows = runCatching { org.json.JSONArray(File(context!!.filesDir,"snapshot.json").readText()) }.getOrNull() ?: return c
        rows.objects().filter {it.text("account")==active}.take(24).forEach { r ->
            val art = File(context!!.filesDir,"snapshot-art/${r.text("key")}.jpg")
            c.addRow(arrayOf<Any?>(1,r.text("account"),r.text("id"),r.text("title"),if (art.isFile) "content://app.aethertv.cinema.snapshot/art/${r.text("key")}" else null,r.optLong("position"),r.optLong("duration"),r.optLong("updated")))
        }
        c.setNotificationUri(context!!.contentResolver,Uri.parse("content://app.aethertv.cinema.snapshot/v1/cards"))
        return c
    }
    override fun openFile(uri: Uri,mode: String): ParcelFileDescriptor {
        require(mode == "r" && uri.pathSegments.size == 2 && uri.pathSegments[0] == "art")
        val key = uri.lastPathSegment ?: error("Missing key")
        require(key.matches(Regex("[a-f0-9]{64}")))
        return ParcelFileDescriptor.open(File(context!!.filesDir,"snapshot-art/$key.jpg"),ParcelFileDescriptor.MODE_READ_ONLY)
    }
    override fun insert(uri: Uri,values: ContentValues?): Uri? = throw UnsupportedOperationException()
    override fun update(uri: Uri,values: ContentValues?,selection: String?,selectionArgs: Array<out String>?) = throw UnsupportedOperationException()
    override fun delete(uri: Uri,selection: String?,selectionArgs: Array<out String>?) = throw UnsupportedOperationException()
}

object Snapshots {
    private var activeId: String? = null
    @Synchronized fun activate(context: android.content.Context,id: String?) {
        if(activeId!=id) clear(context)
        activeId=id
    }
    @Synchronized fun art(context: android.content.Context,account: String,item: String,bitmap: android.graphics.Bitmap) {
        if(activeId!=account) return
        val dir=File(context.filesDir,"snapshot-art").apply{mkdirs()}
        val key=key(account,item)
        val file=File(dir,"$key.tmp")
        file.outputStream().use {out -> android.graphics.Bitmap.createScaledBitmap(bitmap,192,288,true).compress(android.graphics.Bitmap.CompressFormat.JPEG,70,out)}
        check(file.renameTo(File(dir,"$key.jpg")))
    }
    fun key(account: String,item: String): String = java.security.MessageDigest.getInstance("SHA-256").digest("$account:$item".toByteArray()).joinToString("") { "%02x".format(it) }
    fun write(context: android.content.Context,account: Account,items: List<Media>) {
        val rows = org.json.JSONArray()
        items.distinctBy { it.id }.filterNot { it.folder }.take(24).forEach { m -> rows.put(obj("account" to account.id,"id" to m.id,"title" to m.title,"key" to key(account.id,m.id),"position" to m.resume,"duration" to m.ticks,"updated" to System.currentTimeMillis())) }
        synchronized(this) {
            if(activeId!=account.id) return
            val temp = File(context.filesDir,"snapshot.tmp").apply { writeText(rows.toString()) }
            check(temp.renameTo(File(context.filesDir,"snapshot.json")))
        }
        context.contentResolver.notifyChange(Uri.parse("content://app.aethertv.cinema.snapshot/v1/cards"),null)
    }
    @Synchronized fun clear(context: android.content.Context) { File(context.filesDir,"snapshot.json").delete(); File(context.filesDir,"snapshot-art").listFiles()?.forEach { it.delete() }; context.contentResolver.notifyChange(Uri.parse("content://app.aethertv.cinema.snapshot/v1/cards"),null) }
}
