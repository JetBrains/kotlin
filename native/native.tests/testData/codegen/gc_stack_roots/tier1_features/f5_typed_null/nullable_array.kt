// TARGET_BACKEND: NATIVE
// FREE_COMPILER_ARGS: -Xbinary=lateShadowStack=true
@file:OptIn(kotlin.native.runtime.NativeRuntimeApi::class)
package gc_stack_roots.tier1_features.f5_typed_null

import kotlin.native.runtime.GC

class Slot(val id: Int)

fun box(): String {
    val arr = arrayOfNulls<Slot>(20)
    for (i in 0 until 20 step 2) {
        arr[i] = Slot(i)
    }

    GC.collect()

    for (i in 0 until 20) {
        if (i % 2 == 0) {
            if (arr[i]?.id != i) return "FAIL even index $i"
        } else {
            if (arr[i] != null) return "FAIL odd index $i not null"
        }
    }

    return "OK"
}
