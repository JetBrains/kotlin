// TARGET_BACKEND: NATIVE
// FREE_COMPILER_ARGS: -Xbinary=lateShadowStack=true
@file:OptIn(kotlin.native.runtime.NativeRuntimeApi::class)
package gc_stack_roots.tier2_boundaries.loops_with_safepoints

import kotlin.native.runtime.GC

class LoopObj(val v: Int)

fun box(): String {
    var sum = 0
    for (i in 0 until 100) {
        val obj = LoopObj(i)
        if (i % 2 == 1) {
            continue
        }
        if (i > 20) {
            break
        }
        GC.collect()
        sum += obj.v
    }

    if (sum != 110) return "FAIL break continue: $sum"
    return "OK"
}
