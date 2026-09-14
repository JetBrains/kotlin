// TARGET_BACKEND: NATIVE
// FREE_COMPILER_ARGS: -Xbinary=lateShadowStack=true
@file:OptIn(kotlin.native.runtime.NativeRuntimeApi::class)
package gc_stack_roots.tier2_boundaries.recursion

import kotlin.native.runtime.GC

class MutualRoot(val parity: String, val n: Int)

fun isEven(n: Int): Boolean {
    val root = MutualRoot("even", n)
    if (n == 0) {
        GC.collect()
        return root.parity == "even"
    }
    return isOdd(n - 1)
}

fun isOdd(n: Int): Boolean {
    val root = MutualRoot("odd", n)
    if (n == 0) {
        GC.collect()
        return false
    }
    return isEven(n - 1)
}

fun box(): String {
    val e = isEven(20)
    val o = isOdd(21)

    if (!e) return "FAIL isEven(20)"
    if (!o) return "FAIL isOdd(21)"

    return "OK"
}
