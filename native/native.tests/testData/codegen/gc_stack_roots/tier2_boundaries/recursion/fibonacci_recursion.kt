// TARGET_BACKEND: NATIVE
// FREE_COMPILER_ARGS: -Xbinary=lateShadowStack=true
@file:OptIn(kotlin.native.runtime.NativeRuntimeApi::class)
package gc_stack_roots.tier2_boundaries.recursion

import kotlin.native.runtime.GC

class FibPayload(val n: Int, val fib: Long)

fun computeFib(n: Int): Long {
    if (n <= 1) return n.toLong()
    val p = FibPayload(n, 0L)
    val a = computeFib(n - 1)
    val b = computeFib(n - 2)
    if (p.n == 10) {
        GC.collect()
    }
    return a + b
}

fun box(): String {
    val res = computeFib(15)
    if (res != 610L) return "FAIL fibonacci: $res"
    return "OK"
}
