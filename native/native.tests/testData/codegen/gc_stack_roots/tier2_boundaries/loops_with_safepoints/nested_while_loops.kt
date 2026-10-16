// TARGET_BACKEND: NATIVE
// FREE_COMPILER_ARGS: -Xbinary=lateShadowStack=true
@file:OptIn(kotlin.native.runtime.NativeRuntimeApi::class)
package gc_stack_roots.tier2_boundaries.loops_with_safepoints

import kotlin.native.runtime.GC

class Cell(val r: Int, val c: Int)

fun box(): String {
    var r = 0
    var count = 0

    while (r < 10) {
        var c = 0
        while (c < 10) {
            val cell = Cell(r, c)
            count++
            c++
        }
        GC.collect()
        r++
    }

    if (count != 100) return "FAIL nested while count: $count"
    return "OK"
}
