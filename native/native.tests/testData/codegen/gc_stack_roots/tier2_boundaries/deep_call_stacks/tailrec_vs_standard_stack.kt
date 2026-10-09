// TARGET_BACKEND: NATIVE
// FREE_COMPILER_ARGS: -Xbinary=lateShadowStack=true
@file:OptIn(kotlin.native.runtime.NativeRuntimeApi::class)
package gc_stack_roots.tier2_boundaries.deep_call_stacks

import kotlin.native.runtime.GC

class AccItem(val total: Long)

tailrec fun tailrecAccumulate(n: Int, acc: AccItem): AccItem {
    if (n <= 0) {
        GC.collect()
        return acc
    }
    return tailrecAccumulate(n - 1, AccItem(acc.total + n))
}

fun box(): String {
    val res = tailrecAccumulate(1000, AccItem(0L))
    if (res.total != 500500L) return "FAIL tailrec: ${res.total}"
    return "OK"
}
