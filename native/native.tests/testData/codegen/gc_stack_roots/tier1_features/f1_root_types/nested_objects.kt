// TARGET_BACKEND: NATIVE
// FREE_COMPILER_ARGS: -Xbinary=lateShadowStack=true
@file:OptIn(kotlin.native.runtime.NativeRuntimeApi::class)
package gc_stack_roots.tier1_features.f1_root_types

import kotlin.native.runtime.GC

class Inner(val id: String)
class Middle(val inner: Inner, val weight: Double)
class Outer(val middle: Middle, val count: Int)

fun box(): String {
    val inner = Inner("test_inner")
    val middle = Middle(inner, 42.5)
    val outer = Outer(middle, 100)

    GC.collect()

    if (outer.count != 100 || outer.middle.weight != 42.5 || outer.middle.inner.id != "test_inner") {
        return "FAIL: nested object structure corrupted"
    }
    return "OK"
}
