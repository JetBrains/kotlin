// TARGET_BACKEND: NATIVE
// FREE_COMPILER_ARGS: -Xbinary=lateShadowStack=true
@file:OptIn(kotlin.native.runtime.NativeRuntimeApi::class)
package gc_stack_roots.tier1_features.f9_frame_abi

import kotlin.native.runtime.GC

class InvariantChecker(val value: Int)

fun verifyParametersInvariant(o1: InvariantChecker, o2: InvariantChecker, o3: InvariantChecker): Int {
    val local1 = InvariantChecker(o1.value + 10)
    val local2 = InvariantChecker(o2.value + 20)

    GC.collect()

    return o1.value + o2.value + o3.value + local1.value + local2.value
}

fun box(): String {
    val a = InvariantChecker(1)
    val b = InvariantChecker(2)
    val c = InvariantChecker(3)

    val sum = verifyParametersInvariant(a, b, c)
    if (sum != 39) return "FAIL parameters invariant: $sum"
    return "OK"
}
