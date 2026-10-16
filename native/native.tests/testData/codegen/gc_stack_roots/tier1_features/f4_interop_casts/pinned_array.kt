// TARGET_BACKEND: NATIVE
// FREE_COMPILER_ARGS: -Xbinary=lateShadowStack=true
@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class, kotlin.native.runtime.NativeRuntimeApi::class)
package gc_stack_roots.tier1_features.f4_interop_casts

import kotlinx.cinterop.pin
import kotlin.native.runtime.GC

fun box(): String {
    val byteArray = ByteArray(64) { (it * 3).toByte() }
    val pinned = byteArray.pin()

    GC.collect()

    for (i in 0 until 64) {
        if (byteArray[i] != (i * 3).toByte()) {
            pinned.unpin()
            return "FAIL: pinned array data modified"
        }
    }

    pinned.unpin()
    return "OK"
}
