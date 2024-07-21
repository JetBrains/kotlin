package org.jetbrains.litmuskt

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.toKString
import platform.posix.errno
import platform.posix.strerror

@OptIn(ExperimentalForeignApi::class)
fun Int.syscallCheck() {
    if (this != 0) {
        val err = strerror(errno)!!.toKString()
        throw IllegalStateException("syscall error: $err")
    }
}
