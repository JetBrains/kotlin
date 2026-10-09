// TARGET_BACKEND: NATIVE
// FREE_COMPILER_ARGS: -Xbinary=lateShadowStack=true
@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class, kotlin.native.runtime.NativeRuntimeApi::class)
package gc_stack_roots.tier1_features.f4_interop_casts

import kotlinx.cinterop.StableRef
import kotlin.native.runtime.GC

class InteropData(val message: String)

fun box(): String {
    val data = InteropData("stable_message")
    val stableRef = StableRef.create(data)

    GC.collect()

    val retrieved = stableRef.get()
    if (retrieved.message != "stable_message") {
        stableRef.dispose()
        return "FAIL: stable ref value corrupted: ${retrieved.message}"
    }

    stableRef.dispose()
    return "OK"
}
