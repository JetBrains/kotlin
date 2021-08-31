@file:Suppress("unused")

import platform.posix.usleep
import dummy.dummyFunction

fun nativeMain() {
    usleep(100)
}

fun nativeMainUsingCInterop() = dummyFunction()
