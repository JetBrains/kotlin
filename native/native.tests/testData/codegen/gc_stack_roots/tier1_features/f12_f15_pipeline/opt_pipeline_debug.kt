// TARGET_BACKEND: NATIVE
// FREE_COMPILER_ARGS: -Xbinary=lateShadowStack=true
// DISABLE_NATIVE: optimizationMode=OPT
@file:OptIn(kotlin.native.runtime.NativeRuntimeApi::class)
package gc_stack_roots.tier1_features.f12_f15_pipeline

import kotlin.native.runtime.GC

class DebugData(val code: Int, val name: String)

fun debugFrameFunction(): Int {
    val a = DebugData(1, "one")
    val b = DebugData(2, "two")
    val c = DebugData(3, "three")

    GC.collect()

    return a.code + b.code + c.code
}

fun box(): String {
    val sum = debugFrameFunction()
    if (sum != 6) return "FAIL debugFrame: $sum"
    return "OK"
}
