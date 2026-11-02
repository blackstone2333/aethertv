// SPDX-License-Identifier: GPL-2.0-only
package app.orchard.tv.cinema

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.LruCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import java.io.File

class Artwork(private val context: Context) {
    private val gate = Semaphore(3)
    private val memory = object: LruCache<String,Bitmap>(6*1024*1024) { override fun sizeOf(key: String,value: Bitmap) = value.allocationByteCount }
    suspend fun load(a: Account,m: Media): Bitmap? = withContext(Dispatchers.IO) {
        val key = Snapshots.key(a.id,m.id+":"+m.imageTag)
        memory.get(key)?.let { return@withContext it }
        gate.withPermit {
            memory.get(key)?.let { return@withPermit it }
            val dir = File(context.cacheDir,"posters").apply { mkdirs() }
            val file = File(dir,key)
            val bytes = runCatching {
                if (file.exists()) file.readBytes() else provider(a.backend).image(a,m).also { file.writeBytes(it) }
            }.getOrNull() ?: return@withPermit null
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds=true }
            BitmapFactory.decodeByteArray(bytes,0,bytes.size,bounds)
            if (bounds.outWidth !in 1..4096 || bounds.outHeight !in 1..4096) { file.delete(); return@withPermit null }
            val options = BitmapFactory.Options().apply {
                inSampleSize = Integer.highestOneBit((maxOf(bounds.outWidth/320,bounds.outHeight/480)).coerceAtLeast(1))
            }
            val bitmap = BitmapFactory.decodeByteArray(bytes,0,bytes.size,options) ?: return@withPermit null
            memory.put(key,bitmap)
            file.setLastModified(System.currentTimeMillis())
            var size = dir.listFiles()?.sumOf { it.length() } ?: 0
            dir.listFiles()?.sortedBy { it.lastModified() }?.forEach { if (size > 24*1024*1024) { val n=it.length(); if (it.delete()) size-=n } }
            bitmap
        }
    }
    fun clear() { memory.evictAll() }
}
