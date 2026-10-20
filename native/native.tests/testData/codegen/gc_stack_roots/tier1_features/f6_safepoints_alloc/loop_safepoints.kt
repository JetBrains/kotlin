// TARGET_BACKEND: NATIVE
// FREE_COMPILER_ARGS: -Xbinary=lateShadowStack=true
@file:OptIn(kotlin.native.runtime.NativeRuntimeApi::class)
package gc_stack_roots.tier1_features.f6_safepoints_alloc

import kotlin.native.runtime.GC

class Counter(var count: Int)

fun loopWithSafepoints(iterations: Int): Counter {
    val counter = Counter(0)
    for (i in 0 until iterations) {
        counter.count++
        if (i % 1000 == 0) {
            GC.collect()
        }
    }
    return counter
}

fun box(): String {
    val res = loopWithSafepoints(5000)
    if (res.count != 5000) return "FAIL counter: ${res.count}"
    return "OK"
}
