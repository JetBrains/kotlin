// TARGET_BACKEND: NATIVE
// FREE_COMPILER_ARGS: -Xbinary=lateShadowStack=true
@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class, kotlin.native.runtime.NativeRuntimeApi::class)
package gc_stack_roots.tier1_features.f4_interop_casts

import kotlinx.cinterop.*
import kotlin.native.runtime.GC

class ValueHolder(val v: Long)

fun box(): String {
    val holder = ValueHolder(0x123456789ABCDEFL)
    val ref = StableRef.create(holder)
    val rawPtr: COpaquePointer = ref.asCPointer()
    val addr: Long = rawPtr.toLong()

    GC.collect()

    val ptr: COpaquePointer = addr.toCPointer()!!
    val roundTripRef = ptr.asStableRef<ValueHolder>()
    val obj = roundTripRef.get()

    if (obj.v != 0x123456789ABCDEFL) {
        ref.dispose()
        return "FAIL: pointer roundtrip failed"
    }

    ref.dispose()
    return "OK"
}
