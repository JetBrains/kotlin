// TARGET_BACKEND: NATIVE
// FREE_COMPILER_ARGS: -Xbinary=lateShadowStack=true
@file:OptIn(kotlin.native.runtime.NativeRuntimeApi::class)
package gc_stack_roots.tier1_features.f7_root_liveness

import kotlin.native.runtime.GC

class Accumulator(var total: Int)

fun box(): String {
    val acc = Accumulator(0)

    for (i in 1..10) {
        acc.total += i
        GC.collect()
    }

    if (acc.total != 55) return "FAIL loop carried: ${acc.total}"
    return "OK"
}
