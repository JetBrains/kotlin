// TARGET_BACKEND: NATIVE
// FREE_COMPILER_ARGS: -Xbinary=lateShadowStack=true
@file:OptIn(kotlin.native.runtime.NativeRuntimeApi::class)
package gc_stack_roots.tier1_features.f9_frame_abi

import kotlin.native.runtime.GC

class MultiExitObject(val code: Int)

fun functionWithMultipleExits(mode: Int): MultiExitObject {
    val localRoot = MultiExitObject(mode * 10)
    GC.collect()

    when (mode) {
        0 -> return localRoot
        1 -> {
            val branchRoot = MultiExitObject(100)
            GC.collect()
            return branchRoot
        }
        2 -> {
            val altRoot = MultiExitObject(200)
            return altRoot
        }
        else -> return MultiExitObject(999)
    }
}

fun box(): String {
    val r0 = functionWithMultipleExits(0)
    val r1 = functionWithMultipleExits(1)
    val r2 = functionWithMultipleExits(2)
    val r3 = functionWithMultipleExits(3)

    if (r0.code != 0) return "FAIL r0: ${r0.code}"
    if (r1.code != 100) return "FAIL r1: ${r1.code}"
    if (r2.code != 200) return "FAIL r2: ${r2.code}"
    if (r3.code != 999) return "FAIL r3: ${r3.code}"

    return "OK"
}
