// TARGET_BACKEND: NATIVE
// FREE_COMPILER_ARGS: -Xbinary=lateShadowStack=true
@file:OptIn(kotlin.native.runtime.NativeRuntimeApi::class)
package gc_stack_roots.tier1_features.f3_ssa_returns

import kotlin.native.runtime.GC

class Result(val payload: String)

fun createResult(s: String): Result {
    return Result(s)
}

fun box(): String {
    val r = createResult("hello_ssa")
    GC.collect()
    if (r.payload != "hello_ssa") return "FAIL: direct return corrupted"
    return "OK"
}
