// TARGET_BACKEND: NATIVE
// FREE_COMPILER_ARGS: -Xbinary=lateShadowStack=true
@file:OptIn(kotlin.native.runtime.NativeRuntimeApi::class)
package gc_stack_roots.tier1_features.f12_f15_pipeline

import kotlin.native.runtime.GC

class StepPayload(val step: Int)

fun multipleSafepointBlocks(): Int {
    val s1 = StepPayload(1)
    val s2 = StepPayload(2)
    val s3 = StepPayload(3)

    GC.collect()
    GC.collect()

    return s1.step + s2.step + s3.step
}

fun box(): String {
    val res = multipleSafepointBlocks()
    if (res != 6) return "FAIL redundant safepoint: $res"
    return "OK"
}
