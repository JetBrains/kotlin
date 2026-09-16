// WITH_PLATFORM_LIBS
// DISABLE_NATIVE: targetFamily=TVOS
// DISABLE_NATIVE: targetFamily=WATCHOS

import kotlin.test.*
import kotlin.experimental.*
import kotlin.native.runtime.*
import kotlinx.cinterop.*
import platform.posix.*

@ExperimentalForeignApi
private class GzipProbe {
    val marker = "gzip-probe"
}

@Test
@OptIn(ExperimentalNativeApi::class, NativeRuntimeApi::class, ExperimentalForeignApi::class)
fun dumpGzipHasMagic() {
    val file = requireNotNull(tmpfile()) { "Could not open temporary file" }
    val fd = fileno(file)
    assertTrue(fd > -1, "Failed to obtain a temporary file descriptor")
    val local = GzipProbe()
    assertTrue(Debugging.dumpMemory(fd.toLong(), MemoryDumpOptions(gzip = true)))
    assertTrue(hasGzipMagic(fd))
    fclose(file)
}

@Test
@OptIn(ExperimentalNativeApi::class, NativeRuntimeApi::class, ExperimentalForeignApi::class)
fun dumpOmitPayloadsAndGzip() {
    val file = requireNotNull(tmpfile()) { "Could not open temporary file" }
    val fd = fileno(file)
    assertTrue(fd > -1, "Failed to obtain a temporary file descriptor")
    val local = GzipProbe()
    assertTrue(Debugging.dumpMemory(fd.toLong(), MemoryDumpOptions(omitPayloads = true, gzip = true)))
    assertTrue(hasGzipMagic(fd))
    fclose(file)
}

@OptIn(ExperimentalForeignApi::class)
private fun hasGzipMagic(fd: Int): Boolean = memScoped {
    lseek(fd, 0, SEEK_SET)
    val buf = allocArray<UByteVar>(2)
    val n = read(fd, buf, 2.convert())
    n.toLong() == 2L && buf[0] == 0x1fu.toUByte() && buf[1] == 0x8bu.toUByte()
}
