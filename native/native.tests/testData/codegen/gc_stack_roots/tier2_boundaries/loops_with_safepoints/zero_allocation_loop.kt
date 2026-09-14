// TARGET_BACKEND: NATIVE
// FREE_COMPILER_ARGS: -Xbinary=lateShadowStack=true
@file:OptIn(kotlin.native.runtime.NativeRuntimeApi::class)
package gc_stack_roots.tier2_boundaries.loops_with_safepoints

import kotlin.native.runtime.GC

class InvariantRoot(val tag: String)

fun box(): String {
    val root = InvariantRoot("must_remain_valid")
    var counter = 0

    for (i in 0 until 10000) {
        counter++
    }

    GC.collect()

    if (counter != 10000 || root.tag != "must_remain_valid") {
        return "FAIL zero allocation loop: counter=$counter, tag=${root.tag}"
    }
    return "OK"
}
