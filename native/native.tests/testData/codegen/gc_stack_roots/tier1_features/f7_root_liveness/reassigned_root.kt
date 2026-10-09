// TARGET_BACKEND: NATIVE
// FREE_COMPILER_ARGS: -Xbinary=lateShadowStack=true
@file:OptIn(kotlin.experimental.ExperimentalNativeApi::class, kotlin.native.runtime.NativeRuntimeApi::class)
package gc_stack_roots.tier1_features.f7_root_liveness

import kotlin.native.runtime.GC
import kotlin.native.ref.WeakReference

class RefNode(val value: Int)

fun box(): String {
    var current = RefNode(1)
    val weakFirst = WeakReference(current)

    GC.collect()
    if (current.value != 1) return "FAIL initial"

    current = RefNode(2)
    val weakSecond = WeakReference(current)

    GC.collect()

    if (current.value != 2) return "FAIL second"
    if (weakSecond.value == null) return "FAIL second was collected"
    if (weakFirst.value != null) return "FAIL first was NOT collected"

    return "OK"
}
