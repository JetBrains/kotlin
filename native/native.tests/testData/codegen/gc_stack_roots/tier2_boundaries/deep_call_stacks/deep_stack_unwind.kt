// TARGET_BACKEND: NATIVE
// FREE_COMPILER_ARGS: -Xbinary=lateShadowStack=true
@file:OptIn(kotlin.native.runtime.NativeRuntimeApi::class)
package gc_stack_roots.tier2_boundaries.deep_call_stacks

import kotlin.native.runtime.GC

class UnwindMarker(val step: Int)

fun deepUnwindCaller(depth: Int): Int {
    val m = UnwindMarker(depth)
    if (depth == 50) {
        GC.collect()
        throw RuntimeException("bottom_reached")
    }
    val child = deepUnwindCaller(depth + 1)
    return child + m.step
}

fun box(): String {
    try {
        deepUnwindCaller(1)
    } catch (e: RuntimeException) {
        GC.collect()
        if (e.message != "bottom_reached") return "FAIL message: ${e.message}"
        return "OK"
    }
    return "FAIL should have thrown"
}
