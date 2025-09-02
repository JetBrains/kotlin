@file:Suppress("unused")

import dummy.dummyFunction
import platform.posix.usleep

@OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
fun commonMain() {
    dummyFunction()
    usleep(100u)
}
