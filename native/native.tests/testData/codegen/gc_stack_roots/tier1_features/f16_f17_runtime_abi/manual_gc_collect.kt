// TARGET_BACKEND: NATIVE
// FREE_COMPILER_ARGS: -Xbinary=lateShadowStack=true
@file:OptIn(kotlin.native.runtime.NativeRuntimeApi::class)
package gc_stack_roots.tier1_features.f16_f17_runtime_abi

import kotlin.native.runtime.GC

class StackRoot(val value: Long)

fun box(): String {
    val roots = Array(50) { StackRoot(it * 100L) }

    GC.collect()

    for (i in 0 until 50) {
        if (roots[i].value != i * 100L) {
            return "FAIL: root $i corrupted after manual GC"
        }
    }
    return "OK"
}
