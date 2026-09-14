// TARGET_BACKEND: NATIVE
// FREE_COMPILER_ARGS: -Xbinary=lateShadowStack=true
@file:OptIn(kotlin.native.runtime.NativeRuntimeApi::class)
package gc_stack_roots.tier2_boundaries.empty_collections

import kotlin.native.runtime.GC

fun box(): String {
    val emptyList = emptyList<String>()
    val emptyMutable = mutableListOf<Any>()

    GC.collect()

    if (emptyList.size != 0 || !emptyList.isEmpty()) return "FAIL emptyList"
    if (emptyMutable.size != 0 || !emptyMutable.isEmpty()) return "FAIL emptyMutable"

    return "OK"
}
