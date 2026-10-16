// TARGET_BACKEND: NATIVE
// FREE_COMPILER_ARGS: -Xbinary=lateShadowStack=true
@file:OptIn(kotlin.native.runtime.NativeRuntimeApi::class)
package gc_stack_roots.tier1_features.f16_f17_runtime_abi

import kotlin.native.runtime.GC

class MultiFrameRoot(val frameId: Int, val name: String)

fun frameThree(r1: MultiFrameRoot, r2: MultiFrameRoot): Int {
    val r3 = MultiFrameRoot(3, "three")
    GC.collect()
    return r1.frameId + r2.frameId + r3.frameId
}

fun frameTwo(r1: MultiFrameRoot): Int {
    val r2 = MultiFrameRoot(2, "two")
    return frameThree(r1, r2)
}

fun frameOne(): Int {
    val r1 = MultiFrameRoot(1, "one")
    return frameTwo(r1)
}

fun box(): String {
    val sum = frameOne()
    if (sum != 6) return "FAIL multi-frame root set: $sum"
    return "OK"
}
