@file:Suppress("unused")

import platform.posix.usleep
import dummy.dummyFunction

fun main() {
    usleep(100u)
}

fun nativePlatformMainUsingCInterop() = dummyFunction()
