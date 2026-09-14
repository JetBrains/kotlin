// TARGET_BACKEND: NATIVE
// FREE_COMPILER_ARGS: -Xbinary=lateShadowStack=true
// DISABLE_NATIVE: optimizationMode=DEBUG
// DISABLE_NATIVE: optimizationMode=NO
@file:OptIn(kotlin.native.runtime.NativeRuntimeApi::class)
package gc_stack_roots.tier1_features.f12_f15_pipeline

import kotlin.native.runtime.GC

class PipelineNode(val key: Int, var next: PipelineNode? = null)

fun buildChain(n: Int): PipelineNode {
    val head = PipelineNode(0)
    var curr = head
    for (i in 1..n) {
        val next = PipelineNode(i)
        curr.next = next
        curr = next
    }
    return head
}

fun box(): String {
    val chain = buildChain(10)
    GC.collect()

    var count = 0
    var curr: PipelineNode? = chain
    while (curr != null) {
        count++
        curr = curr.next
    }

    if (count != 11) return "FAIL chain length under opt: $count"
    return "OK"
}
