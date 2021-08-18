@file:Suppress("unused")

import platform.posix.usleep
import dummy.dummyFunction


fun commonMain() {
    usleep(100)
}

fun nativeMainParentUsingCInterop() = dummyFunction()
