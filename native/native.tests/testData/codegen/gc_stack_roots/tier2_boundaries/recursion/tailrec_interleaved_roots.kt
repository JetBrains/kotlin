// TARGET_BACKEND: NATIVE
// FREE_COMPILER_ARGS: -Xbinary=lateShadowStack=true
@file:OptIn(kotlin.native.runtime.NativeRuntimeApi::class)
package gc_stack_roots.tier2_boundaries.recursion

import kotlin.native.runtime.GC

class StepWrapper(val step: Int)

tailrec fun tailrecInterleaved(n: Int, w1: StepWrapper, w2: StepWrapper): Int {
    if (n <= 0) {
        GC.collect()
        return w1.step + w2.step
    }
    return tailrecInterleaved(n - 1, StepWrapper(w1.step + 1), StepWrapper(w2.step + 2))
}

fun box(): String {
    val res = tailrecInterleaved(20, StepWrapper(0), StepWrapper(0))
    if (res != 60) return "FAIL tailrec interleaved: $res"
    return "OK"
}
