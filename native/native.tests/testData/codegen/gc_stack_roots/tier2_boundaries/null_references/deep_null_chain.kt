// TARGET_BACKEND: NATIVE
// FREE_COMPILER_ARGS: -Xbinary=lateShadowStack=true
@file:OptIn(kotlin.native.runtime.NativeRuntimeApi::class)
package gc_stack_roots.tier2_boundaries.null_references

import kotlin.native.runtime.GC

class Link(val next: Link?, val v: Int)

fun box(): String {
    val chain = Link(Link(Link(null, 3), 2), 1)

    GC.collect()

    val deepValue = chain.next?.next?.next?.next?.v
    if (deepValue != null) return "FAIL deep null chain did not return null"

    val validValue = chain.next?.next?.v
    if (validValue != 3) return "FAIL valid value in chain: $validValue"

    return "OK"
}
