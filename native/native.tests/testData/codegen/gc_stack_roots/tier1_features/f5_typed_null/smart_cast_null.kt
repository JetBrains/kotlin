// TARGET_BACKEND: NATIVE
// FREE_COMPILER_ARGS: -Xbinary=lateShadowStack=true
@file:OptIn(kotlin.native.runtime.NativeRuntimeApi::class)
package gc_stack_roots.tier1_features.f5_typed_null

import kotlin.native.runtime.GC

class SmartTarget(val number: Int)

fun inspectTarget(target: Any?): Int {
    if (target != null && target is SmartTarget) {
        GC.collect()
        return target.number
    }
    return -1
}

fun box(): String {
    val t = SmartTarget(777)
    val r1 = inspectTarget(t)
    val r2 = inspectTarget(null)
    val r3 = inspectTarget("some string")

    if (r1 != 777) return "FAIL r1: $r1"
    if (r2 != -1) return "FAIL r2: $r2"
    if (r3 != -1) return "FAIL r3: $r3"

    return "OK"
}
