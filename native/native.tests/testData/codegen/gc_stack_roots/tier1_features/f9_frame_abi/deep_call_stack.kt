// TARGET_BACKEND: NATIVE
// FREE_COMPILER_ARGS: -Xbinary=lateShadowStack=true
@file:OptIn(kotlin.native.runtime.NativeRuntimeApi::class)
package gc_stack_roots.tier1_features.f9_frame_abi

import kotlin.native.runtime.GC

class StackPayload(val depth: Int)

fun recursiveCall(depth: Int, maxDepth: Int): StackPayload {
    val localPayload = StackPayload(depth)
    if (depth >= maxDepth) {
        GC.collect()
        return localPayload
    }
    val child = recursiveCall(depth + 1, maxDepth)
    if (localPayload.depth != depth) {
        throw IllegalStateException("localPayload corrupted at depth $depth")
    }
    return child
}

fun box(): String {
    val res = recursiveCall(1, 40)
    if (res.depth != 40) return "FAIL deep stack: ${res.depth}"
    return "OK"
}
