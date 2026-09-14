// TARGET_BACKEND: NATIVE
// FREE_COMPILER_ARGS: -Xbinary=lateShadowStack=true
@file:OptIn(kotlin.native.runtime.NativeRuntimeApi::class)
package gc_stack_roots.tier2_boundaries.empty_collections

import kotlin.native.runtime.GC

fun box(): String {
    val emptySet = emptySet<Double>()

    GC.collect()

    if (emptySet.isNotEmpty() || emptySet.contains(1.0)) {
        return "FAIL emptySet"
    }
    return "OK"
}
