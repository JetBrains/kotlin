// TARGET_BACKEND: NATIVE
// FREE_COMPILER_ARGS: -Xbinary=lateShadowStack=true
@file:OptIn(kotlin.native.runtime.NativeRuntimeApi::class)
package gc_stack_roots.tier2_boundaries.deep_call_stacks

import kotlin.native.runtime.GC

class StackMarker(val id: Int)

fun traverseAndCollect(depth: Int): Int {
    val marker = StackMarker(depth)
    if (depth % 10 == 0) {
        GC.collect()
    }
    if (depth <= 0) return 0
    return marker.id + traverseAndCollect(depth - 1)
}

fun box(): String {
    val total = traverseAndCollect(50)
    if (total != 1275) return "FAIL deep stack GC collection: $total"
    return "OK"
}
