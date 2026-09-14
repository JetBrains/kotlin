// TARGET_BACKEND: NATIVE
// FREE_COMPILER_ARGS: -Xbinary=lateShadowStack=true
@file:OptIn(kotlin.native.runtime.NativeRuntimeApi::class)
package gc_stack_roots.tier1_features.f12_f15_pipeline

import kotlin.native.runtime.GC

class Point(val x: Int, val y: Int)

fun calculateDistanceSq(): Int {
    val p = Point(3, 4)
    GC.collect()
    return p.x * p.x + p.y * p.y
}

fun box(): String {
    val dist = calculateDistanceSq()
    if (dist != 25) return "FAIL escape analysis: $dist"
    return "OK"
}
