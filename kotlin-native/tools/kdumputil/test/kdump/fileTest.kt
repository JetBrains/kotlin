package kdump

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.GZIPOutputStream
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals

class FileTest {
    @Test
    fun maybeDecompressGzip() {
        val raw = "Kotlin/Native dump 1.0.8".encodeToByteArray()
        val compressed = ByteArrayOutputStream().use { baos ->
            GZIPOutputStream(baos).use { it.write(raw) }
            baos.toByteArray()
        }
        val decompressed = ByteArrayInputStream(compressed).maybeDecompress().readBytes()
        assertContentEquals(raw, decompressed)
    }

    @Test
    fun maybeDecompressRaw() {
        val raw = byteArrayOf(0x01, 0x02, 0x03, 0x04)
        val roundTrip = ByteArrayInputStream(raw).maybeDecompress().readBytes()
        assertContentEquals(raw, roundTrip)
    }

    @Test
    fun maybeDecompressEmpty() {
        val roundTrip = ByteArrayInputStream(byteArrayOf()).maybeDecompress().readBytes()
        assertEquals(0, roundTrip.size)
    }
}
