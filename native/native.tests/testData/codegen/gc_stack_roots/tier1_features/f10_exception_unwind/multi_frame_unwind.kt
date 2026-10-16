// TARGET_BACKEND: NATIVE
// FREE_COMPILER_ARGS: -Xbinary=lateShadowStack=true
@file:OptIn(kotlin.native.runtime.NativeRuntimeApi::class)
package gc_stack_roots.tier1_features.f10_exception_unwind

import kotlin.native.runtime.GC

class FrameMarker(val level: Int)

fun deepThrow(level: Int): Nothing {
    val marker = FrameMarker(level)
    if (level < 10) {
        deepThrow(level + 1)
    } else {
        GC.collect()
        throw RuntimeException("Exception at max depth ${marker.level}")
    }
}

fun box(): String {
    val rootMarker = FrameMarker(0)
    try {
        deepThrow(1)
    } catch (e: RuntimeException) {
        GC.collect()
        if (rootMarker.level != 0) return "FAIL rootMarker corrupted"
        if (e.message != "Exception at max depth 10") return "FAIL exception msg: ${e.message}"
        return "OK"
    }
    return "FAIL expected exception"
}
