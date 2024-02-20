package komem.litmus

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.toKString
import platform.posix._SC_NPROCESSORS_ONLN
import platform.posix.errno
import platform.posix.strerror
import platform.posix.sysconf

actual fun cpuCount(): Int = sysconf(_SC_NPROCESSORS_ONLN).toInt()

@OptIn(ExperimentalForeignApi::class)
fun Int.syscallCheck() {
    if (this != 0) {
        val err = strerror(errno)!!.toKString()
        throw IllegalStateException("syscall error: $err")
    }
}
