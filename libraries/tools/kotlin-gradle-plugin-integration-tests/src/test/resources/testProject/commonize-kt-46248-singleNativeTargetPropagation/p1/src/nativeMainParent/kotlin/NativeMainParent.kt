@file:Suppress("unused")

import platform.posix.usleep
import dummy.dummyFunction


fun commonMain() {
    usleep(100u)
}

@OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
fun nativeMainParentUsingCInterop() = dummyFunction()
