// TARGET_BACKEND: NATIVE
// FREE_COMPILER_ARGS: -Xbinary=lateShadowStack=true
@file:OptIn(kotlin.native.runtime.NativeRuntimeApi::class, kotlin.experimental.ExperimentalNativeApi::class)
package gc_stack_roots.tier1_features.f16_f17_runtime_abi

import kotlin.native.runtime.GC
import kotlin.native.ref.WeakReference

class WeakTarget(val name: String)

fun allocateAndDrop(): WeakReference<WeakTarget> {
    val target = WeakTarget("ephemeral")
    val weakRef = WeakReference(target)
    return weakRef
}

fun box(): String {
    val weakRef = allocateAndDrop()
    GC.collect()

    if (weakRef.value != null) {
        return "FAIL: weak reference was not cleared after stack root dropped"
    }
    return "OK"
}
