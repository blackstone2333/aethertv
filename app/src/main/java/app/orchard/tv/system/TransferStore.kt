package app.orchard.tv.system

import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.InputStream
import java.util.UUID

/** A bounded private exchange box, never an HTTP view of the device filesystem. */
class TransferStore(private val directory: File) {
    data class Entry(val id: String, val name: String, val bytes: Long) {
        fun json() = JSONObject().put("id", id).put("name", name).put("bytes", bytes)
    }
    fun entries(): List<Entry> = synchronized(lock) { directory.listFiles { f -> f.extension == "json" }?.mapNotNull { meta ->
        runCatching {
            val o = JSONObject(meta.readText())
            val id = meta.nameWithoutExtension
            val file = data(id) ?: return@runCatching null
            Entry(id, o.getString("name"), file.length())
        }.getOrNull()
    }?.sortedBy { it.name.lowercase() }.orEmpty() }
    fun json() = JSONArray().apply { entries().forEach { put(it.json()) } }

    fun add(input: InputStream, name: String): Entry = synchronized(lock) {
        directory.mkdirs()
        val existing = entries()
        require(existing.size < MAX_FILES) { "exchange box full" }
        val remaining = (MAX_TOTAL - existing.sumOf { it.bytes }).coerceAtLeast(0)
        val id = UUID.randomUUID().toString()
        val part = File(directory, "$id.part")
        val target = File(directory, "$id.data")
        try {
            var size = 0L
            part.outputStream().use { out ->
                val buffer = ByteArray(32 * 1024)
                while (true) {
                    val count = input.read(buffer)
                    if (count < 0) break
                    size += count
                    require(size <= minOf(MAX_FILE, remaining)) { "file limit" }
                    out.write(buffer, 0, count)
                }
            }
            require(size > 0) { "empty file" }
            check(part.renameTo(target))
            val safe = name.substringAfterLast('/').substringAfterLast('\\')
                .filter { it >= ' ' && it != '\u007f' && it !in "\"<>:" }.take(160).ifBlank { "file" }
            File(directory, "$id.json").writeText(JSONObject().put("name", safe).toString())
            return Entry(id, safe, size)
        } catch (e: Exception) {
            part.delete(); target.delete(); File(directory, "$id.json").delete(); throw e
        }
    }
    fun data(id: String): File? = if (ID.matches(id)) File(directory, "$id.data").takeIf { it.isFile } else null
    fun delete(id: String): Boolean = synchronized(lock) {
        val file = data(id) ?: return false
        if (!file.delete()) return false
        File(directory, "$id.json").delete()
        return true
    }
    companion object {
        private val lock = Any()
        const val MAX_FILE = 100L * 1024 * 1024
        const val MAX_TOTAL = 512L * 1024 * 1024
        const val MAX_FILES = 64
        private val ID = Regex("[a-f0-9]{8}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{12}")
    }
}
