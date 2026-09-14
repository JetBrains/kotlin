// TARGET_BACKEND: NATIVE
// FREE_COMPILER_ARGS: -Xbinary=lateShadowStack=true
@file:OptIn(kotlin.experimental.ExperimentalNativeApi::class, kotlin.native.runtime.NativeRuntimeApi::class)
package gc_stack_roots.tier1_features.f7_root_liveness

import kotlin.native.runtime.GC
import kotlin.native.ref.WeakReference

class DiamondData(val label: String)

fun testDiamond(takeTrueBranch: Boolean): String {
    val objA = DiamondData("A")
    val objB = DiamondData("B")
    val weakA = WeakReference(objA)
    val weakB = WeakReference(objB)

    val chosen = if (takeTrueBranch) {
        objA
    } else {
        objB
    }

    GC.collect()

    return "${chosen.label};${weakA.value != null};${weakB.value != null}"
}

fun box(): String {
    val r1 = testDiamond(true)
    val r2 = testDiamond(false)

    if (!r1.startsWith("A;true;")) return "FAIL diamond r1: $r1"
    if (!r2.startsWith("B;")) return "FAIL diamond r2: $r2"

    return "OK"
}
