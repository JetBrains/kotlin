// TARGET_BACKEND: NATIVE
// FREE_COMPILER_ARGS: -Xbinary=lateShadowStack=true
@file:OptIn(kotlin.native.runtime.NativeRuntimeApi::class)
package gc_stack_roots.tier1_features.f11_addrspace_lowering

import kotlin.native.runtime.GC

fun box(): String {
    val strings = Array(10) { "init_$it" }
    for (i in 0 until 10) {
        strings[i] = "updated_$i"
    }

    GC.collect()

    for (i in 0 until 10) {
        if (strings[i] != "updated_$i") {
            return "FAIL array index $i: ${strings[i]}"
        }
    }
    return "OK"
}
