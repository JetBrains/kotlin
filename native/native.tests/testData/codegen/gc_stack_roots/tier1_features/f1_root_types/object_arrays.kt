// TARGET_BACKEND: NATIVE
// FREE_COMPILER_ARGS: -Xbinary=lateShadowStack=true
@file:OptIn(kotlin.native.runtime.NativeRuntimeApi::class)
package gc_stack_roots.tier1_features.f1_root_types

import kotlin.native.runtime.GC

class Element(val index: Int, val name: String)

fun box(): String {
    val arr = Array(50) { i -> Element(i, "item_$i") }

    GC.collect()

    for (i in 0 until 50) {
        if (arr[i].index != i || arr[i].name != "item_$i") {
            return "FAIL: array element $i corrupted"
        }
    }
    return "OK"
}
