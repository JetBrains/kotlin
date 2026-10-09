// TARGET_BACKEND: NATIVE
// FREE_COMPILER_ARGS: -Xbinary=lateShadowStack=true -Xbinary=lateShadowStackClearDeadSlots=true
@file:OptIn(kotlin.experimental.ExperimentalNativeApi::class, kotlin.native.runtime.NativeRuntimeApi::class)
package gc_stack_roots.tier1_features.f16_f17_runtime_abi

import kotlin.native.runtime.GC
import kotlin.native.ref.WeakReference

class CycleNode(val id: Int, var partner: CycleNode? = null)

fun createDetachedCycle(): WeakReference<CycleNode> {
    val n1 = CycleNode(1)
    val n2 = CycleNode(2)
    n1.partner = n2
    n2.partner = n1
    return WeakReference(n1)
}

fun box(): String {
    val weakCycle = createDetachedCycle()

    GC.collect()

    if (weakCycle.value != null) {
        return "FAIL: detached cycle was not collected"
    }
    return "OK"
}
