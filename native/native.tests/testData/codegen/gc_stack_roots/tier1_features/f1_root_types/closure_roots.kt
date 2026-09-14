// TARGET_BACKEND: NATIVE
// FREE_COMPILER_ARGS: -Xbinary=lateShadowStack=true
@file:OptIn(kotlin.native.runtime.NativeRuntimeApi::class)
package gc_stack_roots.tier1_features.f1_root_types

import kotlin.native.runtime.GC

class Captured(var value: Int)

fun makeClosure(initial: Int): () -> Int {
    val captured = Captured(initial)
    return {
        GC.collect()
        captured.value += 10
        captured.value
    }
}

fun box(): String {
    val fn = makeClosure(5)
    val r1 = fn()
    val r2 = fn()

    if (r1 != 15 || r2 != 25) {
        return "FAIL: closure captured state corrupted: r1=$r1, r2=$r2"
    }
    return "OK"
}
