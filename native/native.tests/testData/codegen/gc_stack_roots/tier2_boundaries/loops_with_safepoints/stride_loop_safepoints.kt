// TARGET_BACKEND: NATIVE
// FREE_COMPILER_ARGS: -Xbinary=lateShadowStack=true
@file:OptIn(kotlin.native.runtime.NativeRuntimeApi::class)
package gc_stack_roots.tier2_boundaries.loops_with_safepoints

import kotlin.native.runtime.GC

class StrideItem(val index: Int)

fun box(): String {
    var sum = 0
    for (i in 0 until 100 step 7) {
        val item = StrideItem(i)
        sum += item.index
        if (i % 21 == 0) {
            GC.collect()
        }
    }

    if (sum != 735) return "FAIL stride loop: $sum"
    return "OK"
}
