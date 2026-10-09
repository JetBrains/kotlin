// TARGET_BACKEND: NATIVE
// FREE_COMPILER_ARGS: -Xbinary=lateShadowStack=true
@file:OptIn(kotlin.experimental.ExperimentalNativeApi::class, kotlin.native.runtime.NativeRuntimeApi::class)
package gc_stack_roots.tier1_features.f7_root_liveness

import kotlin.native.runtime.GC
import kotlin.native.ref.WeakReference

class LiveTarget(val payload: String)

fun testLiveRetention(): Boolean {
    val liveTarget = LiveTarget("must_survive")
    val weakRef = WeakReference(liveTarget)

    GC.collect()

    val retrieved = liveTarget.payload
    return weakRef.value != null && retrieved == "must_survive"
}

fun box(): String {
    if (!testLiveRetention()) {
        return "FAIL: live reference should survive GC"
    }
    return "OK"
}
