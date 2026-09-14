// TARGET_BACKEND: NATIVE
// FREE_COMPILER_ARGS: -Xbinary=lateShadowStack=true
@file:OptIn(kotlin.native.runtime.NativeRuntimeApi::class)
package gc_stack_roots.tier2_boundaries.large_live_roots

import kotlin.native.runtime.GC

class PressureObject(val x: Long, val y: Long)

fun box(): String {
    val p0 = PressureObject(1, 2)
    val p1 = PressureObject(3, 4)
    val p2 = PressureObject(5, 6)
    val p3 = PressureObject(7, 8)
    val p4 = PressureObject(9, 10)
    val p5 = PressureObject(11, 12)
    val p6 = PressureObject(13, 14)
    val p7 = PressureObject(15, 16)

    GC.collect()

    val sum = (p0.x + p0.y) + (p1.x + p1.y) + (p2.x + p2.y) + (p3.x + p3.y) +
              (p4.x + p4.y) + (p5.x + p5.y) + (p6.x + p6.y) + (p7.x + p7.y)

    if (sum != 136L) return "FAIL register pressure: $sum"
    return "OK"
}
