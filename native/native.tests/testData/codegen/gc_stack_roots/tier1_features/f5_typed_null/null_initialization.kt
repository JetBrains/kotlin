// TARGET_BACKEND: NATIVE
// FREE_COMPILER_ARGS: -Xbinary=lateShadowStack=true
@file:OptIn(kotlin.native.runtime.NativeRuntimeApi::class)
package gc_stack_roots.tier1_features.f5_typed_null

import kotlin.native.runtime.GC

class DummyRef(val x: Int)

fun box(): String {
    var ref: DummyRef? = null
    GC.collect()

    if (ref != null) return "FAIL: ref should be null initially"

    ref = DummyRef(42)
    GC.collect()

    if (ref.x != 42) return "FAIL: ref value should be 42"

    ref = null
    GC.collect()

    if (ref != null) return "FAIL: ref should be null after clearing"
    return "OK"
}
