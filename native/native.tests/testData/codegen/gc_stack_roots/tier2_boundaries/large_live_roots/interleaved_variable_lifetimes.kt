// TARGET_BACKEND: NATIVE
// FREE_COMPILER_ARGS: -Xbinary=lateShadowStack=true
@file:OptIn(kotlin.native.runtime.NativeRuntimeApi::class)
package gc_stack_roots.tier2_boundaries.large_live_roots

import kotlin.native.runtime.GC

class Interleaved(val tag: String, val num: Int)

fun box(): String {
    val a = Interleaved("a", 1)
    val b = Interleaved("b", 2)
    val c = Interleaved("c", 3)

    GC.collect()

    val d = Interleaved("d", a.num + 10)
    val e = Interleaved("e", b.num + 20)

    GC.collect()

    val total = a.num + b.num + c.num + d.num + e.num
    if (total != 39) return "FAIL interleaved lifetimes: $total"
    return "OK"
}
