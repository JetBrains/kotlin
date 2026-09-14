// TARGET_BACKEND: NATIVE
// FREE_COMPILER_ARGS: -Xbinary=lateShadowStack=true
@file:OptIn(kotlin.native.runtime.NativeRuntimeApi::class)
package gc_stack_roots.tier1_features.f10_exception_unwind

import kotlin.native.runtime.GC

class MiddleMarker(val name: String)

fun rethrowHelper(): Nothing {
    throw IllegalArgumentException("origin_error")
}

fun middleCatchAndRethrow(): Nothing {
    val mid = MiddleMarker("mid_level")
    try {
        rethrowHelper()
    } catch (e: IllegalArgumentException) {
        GC.collect()
        if (mid.name != "mid_level") throw IllegalStateException("corrupted mid")
        throw e
    }
}

fun box(): String {
    try {
        middleCatchAndRethrow()
    } catch (e: IllegalArgumentException) {
        GC.collect()
        if (e.message != "origin_error") return "FAIL rethrow message"
        return "OK"
    }
    return "FAIL should have thrown"
}
