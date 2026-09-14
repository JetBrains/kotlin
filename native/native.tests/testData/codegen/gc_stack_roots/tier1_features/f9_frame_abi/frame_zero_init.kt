// TARGET_BACKEND: NATIVE
// FREE_COMPILER_ARGS: -Xbinary=lateShadowStack=true
@file:OptIn(kotlin.native.runtime.NativeRuntimeApi::class)
package gc_stack_roots.tier1_features.f9_frame_abi

import kotlin.native.runtime.GC

class ValidPointerCheck(val id: Int)

fun frameFunctionWithMultipleRoots(): ValidPointerCheck {
    val r1 = ValidPointerCheck(1)
    val r2 = ValidPointerCheck(2)
    val r3 = ValidPointerCheck(3)

    GC.collect()

    if (r1.id != 1 || r2.id != 2 || r3.id != 3) {
        throw IllegalStateException("Roots corrupted in frame")
    }
    return r1
}

fun box(): String {
    val res = frameFunctionWithMultipleRoots()
    if (res.id != 1) return "FAIL frame_zero_init"
    return "OK"
}
