package app.orchard.tv.live.repo

import java.io.File
import java.io.IOException
import java.security.MessageDigest

data class CachedPayload(
    val bytes: ByteArray,
    val storedAtMillis: Long,
    val fresh: Boolean,
)

/** Small file cache for playlists and EPG feeds. Stale entries remain available for offline use. */
class LiveCacheStore(
    private val directory: File,
    private val ttlMillis: Long = 15 * 60 * 1000L,
    private val maxEntryBytes: Long = 16L * 1024 * 1024,
    private val maxTotalBytes: Long = 48L * 1024 * 1024,
    private val nowMillis: () -> Long = { System.currentTimeMillis() },
) {
    init {
        require(ttlMillis >= 0) { "ttlMillis must be non-negative" }
        require(maxEntryBytes > 0 && maxTotalBytes >= maxEntryBytes) { "invalid cache limits" }
    }

    fun read(key: String, now: Long = nowMillis()): CachedPayload? {
        val file = fileFor(key)
        if (!file.isFile || file.length() > maxEntryBytes) return null
        val stored = file.lastModified().takeIf { it > 0 } ?: return null
        val bytes = runCatching { file.readBytes() }.getOrNull() ?: return null
        return CachedPayload(bytes, stored, now - stored <= ttlMillis)
    }

    fun write(key: String, bytes: ByteArray, now: Long = nowMillis()): Boolean {
        if (bytes.size.toLong() > maxEntryBytes) return false
        directory.mkdirs()
        val destination = fileFor(key)
        val temporary = File(directory, ".${destination.name}.tmp-${System.nanoTime()}")
        return try {
            temporary.outputStream().use { it.write(bytes) }
            if (!temporary.renameTo(destination)) {
                destination.delete()
                if (!temporary.renameTo(destination)) throw IOException("cannot move cache entry")
            }
            destination.setLastModified(now)
            prune()
            true
        } catch (_: IOException) {
            temporary.delete()
            false
        }
    }

    fun remove(key: String) { fileFor(key).delete() }

    fun clear() { directory.listFiles()?.forEach { it.delete() } }

    private fun prune() {
        val files = directory.listFiles { file ->
            file.isFile && !file.name.endsWith(".tmp") && !file.name.contains(".tmp-")
        }?.toMutableList() ?: return
        var total = files.sumOf { it.length() }
        files.sortBy { it.lastModified() }
        while (total > maxTotalBytes && files.isNotEmpty()) {
            val victim = files.removeAt(0)
            total -= victim.length()
            victim.delete()
        }
    }

    private fun fileFor(key: String): File = File(directory, "${sha256(key)}.bin")

    private fun sha256(value: String): String =
        MessageDigest.getInstance("SHA-256").digest(value.toByteArray(Charsets.UTF_8)).joinToString("") { "%02x".format(it) }
}
