// TARGET_BACKEND: NATIVE
// FREE_COMPILER_ARGS: -Xbinary=lateShadowStack=true
@file:OptIn(kotlin.native.runtime.NativeRuntimeApi::class)
package gc_stack_roots.tier1_features.f8_slot_sharing

import kotlin.native.runtime.GC

class IterationPayload(val iter: Int, val tag: String)

fun processIterations(count: Int): Int {
    var sum = 0
    for (i in 0 until count) {
        val item = IterationPayload(i, "tag_$i")
        sum += item.iter
        if (i % 5 == 0) {
            GC.collect()
        }
    }
    return sum
}

fun box(): String {
    val total = processIterations(20)
    if (total != 190) return "FAIL loop iteration sharing: $total"
    return "OK"
}
