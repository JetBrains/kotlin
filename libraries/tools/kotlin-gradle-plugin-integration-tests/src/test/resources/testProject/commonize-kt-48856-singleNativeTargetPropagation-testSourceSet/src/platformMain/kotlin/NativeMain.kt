@file:Suppress("unused")

import sampleInterop.sampleInterop

object NativeMain {
    @OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
    fun x() = sampleInterop()
}