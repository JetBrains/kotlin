// TARGET_BACKEND: NATIVE
// FREE_COMPILER_ARGS: -Xbinary=lateShadowStack=true
@file:OptIn(kotlin.native.runtime.NativeRuntimeApi::class)
package gc_stack_roots.tier2_boundaries.empty_collections

import kotlin.native.runtime.GC

fun box(): String {
    val iter = emptyList<String>().iterator()

    GC.collect()

    if (iter.hasNext()) {
        return "FAIL empty iterator hasNext"
    }
    return "OK"
}
