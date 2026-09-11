package kdump

import java.io.File
import java.io.InputStream
import java.io.PushbackInputStream
import java.util.zip.GZIPInputStream

private val GZIP_MAGIC = byteArrayOf(0x1f.toByte(), 0x8b.toByte())

/**
 * Wraps [this] in a [GZIPInputStream] if the stream starts with the gzip magic bytes,
 * otherwise returns the stream unchanged.
 */
fun InputStream.maybeDecompress(): InputStream {
    val pb = PushbackInputStream(this, 2)
    val header = ByteArray(2)
    val read = pb.read(header)
    return if (read == 2 && header[0] == GZIP_MAGIC[0] && header[1] == GZIP_MAGIC[1]) {
        pb.unread(header, 0, read)
        GZIPInputStream(pb)
    } else {
        if (read > 0) pb.unread(header, 0, read)
        pb
    }
}

fun File.readDump() =
        inputStream()
                .maybeDecompress()
                .buffered()
                .run { PushbackInputStream(this) }
                .readDump()
