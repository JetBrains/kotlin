// TARGET_BACKEND: NATIVE
// FREE_COMPILER_ARGS: -Xbinary=lateShadowStack=true
@file:OptIn(kotlin.native.runtime.NativeRuntimeApi::class)
package gc_stack_roots.tier1_features.f6_safepoints_alloc

import kotlin.native.runtime.GC

class MatrixCell(val r: Int, val c: Int, var v: Int)

fun traverseMatrix(rows: Int, cols: Int): Int {
    var sum = 0
    for (r in 0 until rows) {
        val rowCells = Array(cols) { c -> MatrixCell(r, c, r * cols + c) }
        for (c in 0 until cols) {
            sum += rowCells[c].v
        }
        if (r % 10 == 0) {
            GC.collect()
        }
    }
    return sum
}

fun box(): String {
    val total = traverseMatrix(20, 20)
    if (total != 79800) return "FAIL matrix sum: $total"
    return "OK"
}
