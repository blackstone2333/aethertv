package app.orchard.tv.system

import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.io.File
import java.io.InputStream
import java.io.RandomAccessFile
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class TransferStoreTest {
    private val context = RuntimeEnvironment.getApplication()
    private val root = File(context.filesDir, "exchange-test-${UUID.randomUUID()}")
    private val store = TransferStore(root)

    @Test fun transferRoundTripSurvivesNewStoreAndSanitizesDisplayName() {
        val payload = "a small shared file".toByteArray()
        val added = store.add(payload.inputStream(), "../../shared/phone.txt")
        assertEquals("phone.txt", added.name)
        assertArrayEquals(payload, TransferStore(root).data(added.id)!!.readBytes())
        assertEquals(listOf(added), TransferStore(root).entries())
    }

    @Test fun traversalCannotReadOrDeleteOwnerFiles() {
        val owner = File(context.filesDir, "owner-test.txt").apply { writeText("keep") }
        assertNull(store.data("../owner-test.txt"))
        assertFalse(store.delete("../owner-test.txt"))
        val added = store.add("shared".byteInputStream(), "share.txt")
        assertTrue(store.delete(added.id))
        assertTrue(owner.exists())
        assertTrue(store.entries().isEmpty())
    }

    @Test fun interruptedCopyDoesNotPublishOrLeakTemporaryFiles() {
        val broken = object : InputStream() {
            var count = 0
            override fun read(): Int { if (++count > 8) throw java.io.IOException("interrupted"); return 65 }
        }
        assertThrows(java.io.IOException::class.java) { store.add(broken, "broken.txt") }
        assertTrue(store.entries().isEmpty())
        assertTrue(root.listFiles().orEmpty().isEmpty())
    }

    @Test fun quotaRejectsNewFilesWithoutEvictingAnyExistingFile() {
        root.mkdirs()
        repeat(6) { i ->
            val id = UUID.randomUUID().toString()
            File(root, "$id.json").writeText("{\"name\":\"$i.bin\"}")
            RandomAccessFile(File(root, "$id.data"), "rw").use { it.setLength(90L * 1024 * 1024) }
        }
        val before = store.entries()
        assertThrows(IllegalArgumentException::class.java) { store.add("overflow".byteInputStream(), "extra.txt") }
        assertEquals(before, store.entries())
    }

    @Test fun emptyFileIsRejected() {
        assertThrows(IllegalArgumentException::class.java) { store.add(ByteArray(0).inputStream(), "empty.txt") }
        assertTrue(store.entries().isEmpty())
    }
}
