// TARGET_BACKEND: NATIVE
// FREE_COMPILER_ARGS: -Xbinary=lateShadowStack=true
@file:OptIn(kotlin.native.runtime.NativeRuntimeApi::class)
package gc_stack_roots.tier2_boundaries.large_live_roots

import kotlin.native.runtime.GC

class ArrayData(val index: Int)

fun box(): String {
    val size = 500
    val data = Array(size) { ArrayData(it) }

    var sum = 0
    for (i in 0 until size) {
        sum += data[i].index
        if (i % 100 == 0) {
            GC.collect()
        }
    }

    val expected = (size - 1) * size / 2
    if (sum != expected) return "FAIL array traversal sum: $sum, expected: $expected"
    return "OK"
}
