// TARGET_BACKEND: NATIVE
// FREE_COMPILER_ARGS: -Xbinary=lateShadowStack=true
@file:OptIn(kotlin.native.runtime.NativeRuntimeApi::class)
package gc_stack_roots.tier1_features.f12_f15_pipeline

import kotlin.native.runtime.GC

class InlinedData(val str: String)

inline fun <R> executeWithSafepoint(block: () -> R): R {
    GC.collect()
    return block()
}

fun box(): String {
    val d = InlinedData("inside_inline")
    val res = executeWithSafepoint {
        d.str + "_executed"
    }

    if (res != "inside_inline_executed") return "FAIL inline opt pipeline: $res"
    return "OK"
}
