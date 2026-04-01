@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

import kotlinx.cinterop.*

actual fun env(name: String): String? {
    return platform.posix.getenv(name)?.toKString()
}

actual fun launchProcess(command: String): Int {
    return platform.posix.system(command)
}