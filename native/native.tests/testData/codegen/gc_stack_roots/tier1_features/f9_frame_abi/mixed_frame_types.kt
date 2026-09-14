// TARGET_BACKEND: NATIVE
// FREE_COMPILER_ARGS: -Xbinary=lateShadowStack=true
@file:OptIn(kotlin.native.runtime.NativeRuntimeApi::class)
package gc_stack_roots.tier1_features.f9_frame_abi

import kotlin.native.runtime.GC

class RefData(val s: String)

fun mixedFrameFunction(p1: Int, p2: Double, p3: RefData, p4: Long): String {
    val l1 = p1 * 10
    val l2 = RefData("local_${p3.s}")
    val l3 = p2 + 5.5
    val l4 = RefData("another_${p4}")

    GC.collect()

    return "$l1;${l2.s};$l3;${l4.s}"
}

fun box(): String {
    val arg = RefData("input")
    val res = mixedFrameFunction(5, 2.5, arg, 100L)
    if (res != "50;local_input;8.0;another_100") {
        return "FAIL mixedFrame: $res"
    }
    return "OK"
}
