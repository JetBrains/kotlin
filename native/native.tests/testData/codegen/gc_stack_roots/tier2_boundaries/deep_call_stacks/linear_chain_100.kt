// TARGET_BACKEND: NATIVE
// FREE_COMPILER_ARGS: -Xbinary=lateShadowStack=true
@file:OptIn(kotlin.native.runtime.NativeRuntimeApi::class)
package gc_stack_roots.tier2_boundaries.deep_call_stacks

import kotlin.native.runtime.GC

class StackNode(val depth: Int)

fun recursiveChain(current: Int, target: Int): StackNode {
    val node = StackNode(current)
    if (current == target) {
        GC.collect()
        return node
    }
    val res = recursiveChain(current + 1, target)
    if (node.depth != current) {
        throw IllegalStateException("corrupted frame at depth $current")
    }
    return res
}

fun box(): String {
    val node = recursiveChain(1, 100)
    if (node.depth != 100) return "FAIL linear chain 100: ${node.depth}"
    return "OK"
}
