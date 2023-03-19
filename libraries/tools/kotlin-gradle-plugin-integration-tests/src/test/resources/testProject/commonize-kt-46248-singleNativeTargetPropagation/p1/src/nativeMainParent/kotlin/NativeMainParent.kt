@file:Suppress("unused")

import platform.posix.usleep
import dummy.dummyFunction


fun commonMain() {
    usleep(100u)
}

fun nativeMainParentUsingCInterop() = dummyFunction()
