// TARGET_BACKEND: NATIVE
// FREE_COMPILER_ARGS: -Xbinary=lateShadowStack=true
@file:OptIn(kotlin.native.runtime.NativeRuntimeApi::class)
package gc_stack_roots.tier2_boundaries.empty_collections

import kotlin.native.runtime.GC

fun box(): String {
    val emptyStrings = emptyArray<String>()
    val emptyInts = IntArray(0)

    GC.collect()

    if (emptyStrings.size != 0) return "FAIL emptyStrings size: ${emptyStrings.size}"
    if (emptyInts.size != 0) return "FAIL emptyInts size: ${emptyInts.size}"

    return "OK"
}
