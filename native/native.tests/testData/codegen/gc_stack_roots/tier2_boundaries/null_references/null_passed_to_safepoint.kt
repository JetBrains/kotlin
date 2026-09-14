// TARGET_BACKEND: NATIVE
// FREE_COMPILER_ARGS: -Xbinary=lateShadowStack=true
@file:OptIn(kotlin.native.runtime.NativeRuntimeApi::class)
package gc_stack_roots.tier2_boundaries.null_references

import kotlin.native.runtime.GC

fun processNullableRoots(r1: Any?, r2: Any?, r3: Any?): Int {
    GC.collect()
    var count = 0
    if (r1 != null) count++
    if (r2 != null) count++
    if (r3 != null) count++
    return count
}

fun box(): String {
    val countAllNull = processNullableRoots(null, null, null)
    val countMixed = processNullableRoots("a", null, "c")

    if (countAllNull != 0) return "FAIL all null count: $countAllNull"
    if (countMixed != 2) return "FAIL mixed count: $countMixed"

    return "OK"
}
