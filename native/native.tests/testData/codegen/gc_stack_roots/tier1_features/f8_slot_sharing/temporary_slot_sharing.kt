// TARGET_BACKEND: NATIVE
// FREE_COMPILER_ARGS: -Xbinary=lateShadowStack=true
@file:OptIn(kotlin.native.runtime.NativeRuntimeApi::class)
package gc_stack_roots.tier1_features.f8_slot_sharing

import kotlin.native.runtime.GC

class TempVal(val v: Int)

fun sumTemporaries(): Int {
    val t1 = TempVal(10)
    val v1 = t1.v

    val t2 = TempVal(20)
    val v2 = t2.v

    val t3 = TempVal(30)
    val v3 = t3.v

    GC.collect()

    return v1 + v2 + v3
}

fun box(): String {
    val s = sumTemporaries()
    if (s != 60) return "FAIL sumTemporaries: $s"
    return "OK"
}
