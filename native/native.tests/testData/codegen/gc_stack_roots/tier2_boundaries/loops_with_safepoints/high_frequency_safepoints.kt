// TARGET_BACKEND: NATIVE
// FREE_COMPILER_ARGS: -Xbinary=lateShadowStack=true
@file:OptIn(kotlin.native.runtime.NativeRuntimeApi::class)
package gc_stack_roots.tier2_boundaries.loops_with_safepoints

import kotlin.native.runtime.GC

class StepInfo(val step: Int)

fun box(): String {
    var lastStep: StepInfo? = null

    for (i in 0 until 50) {
        val curr = StepInfo(i)
        GC.collect()
        lastStep = curr
    }

    if (lastStep?.step != 49) return "FAIL high frequency safepoints: ${lastStep?.step}"
    return "OK"
}
