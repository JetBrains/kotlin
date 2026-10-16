// TARGET_BACKEND: NATIVE
// FREE_COMPILER_ARGS: -Xbinary=lateShadowStack=true
@file:OptIn(kotlin.experimental.ExperimentalNativeApi::class, kotlin.native.runtime.NativeRuntimeApi::class)
package gc_stack_roots.tier3_combinations

import kotlin.native.runtime.GC
import kotlin.native.ref.WeakReference

class BranchData(val id: Int)

fun testBranchingLiveness(takeTrue: Boolean): Pair<Boolean, Boolean> {
    var weak1: WeakReference<BranchData>? = null
    var weak2: WeakReference<BranchData>? = null

    val retained: BranchData
    if (takeTrue) {
        val o1 = BranchData(1)
        weak1 = WeakReference(o1)
        retained = o1
    } else {
        val o2 = BranchData(2)
        weak2 = WeakReference(o2)
        retained = o2
    }

    GC.collect()

    return Pair(retained.id == 1, retained.id == 2)
}

fun box(): String {
    val (is1True, is2True) = testBranchingLiveness(true)
    val (is1False, is2False) = testBranchingLiveness(false)

    if (!is1True || is2True) return "FAIL branch true: $is1True, $is2True"
    if (is1False || !is2False) return "FAIL branch false: $is1False, $is2False"

    return "OK"
}
