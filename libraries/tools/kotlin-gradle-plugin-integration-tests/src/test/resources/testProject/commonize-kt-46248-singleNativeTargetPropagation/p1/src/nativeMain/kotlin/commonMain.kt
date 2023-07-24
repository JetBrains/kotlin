@file:Suppress("unused")

import platform.posix.usleep
import dummy.dummyFunction

fun nativeMain() {
    usleep(100u)
}

@OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
fun nativeMainUsingCInterop() = dummyFunction()
