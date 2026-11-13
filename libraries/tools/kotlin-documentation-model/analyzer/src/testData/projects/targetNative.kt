// Kotlin/Native-specific declarations: C interop pointers, thread-local and shared state, and native concurrency.

// MODULE: nativeMain
// PLATFORM: NATIVE
// FILE: pkg/Native.kt
@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class, kotlin.experimental.ExperimentalNativeApi::class)

package pkg

import kotlin.concurrent.AtomicInt
import kotlin.native.concurrent.ThreadLocal
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.IntVar
import kotlinx.cinterop.pointed
import kotlinx.cinterop.value

@ThreadLocal
object NativeCache {
    var hits: Int = 0
}

class NativeCounter {
    val value: AtomicInt = AtomicInt(0)

    fun increment(): Int = value.incrementAndGet()
}

fun readInt(pointer: CPointer<IntVar>): Int = pointer.pointed.value

fun nativeOs(): OsFamily = Platform.osFamily
