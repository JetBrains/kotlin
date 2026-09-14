// TARGET_BACKEND: NATIVE
// FREE_COMPILER_ARGS: -Xbinary=lateShadowStack=true
@file:OptIn(kotlin.native.runtime.NativeRuntimeApi::class)
package gc_stack_roots.tier2_boundaries.null_references

import kotlin.native.runtime.GC

fun box(): String {
    val arr = arrayOfNulls<String>(1000)

    GC.collect()

    for (i in 0 until 1000) {
        if (arr[i] != null) return "FAIL element $i not null"
    }
    return "OK"
}
