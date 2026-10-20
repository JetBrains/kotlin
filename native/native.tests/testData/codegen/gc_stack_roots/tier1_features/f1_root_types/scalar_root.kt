// TARGET_BACKEND: NATIVE
// FREE_COMPILER_ARGS: -Xbinary=lateShadowStack=true
@file:OptIn(kotlin.native.runtime.NativeRuntimeApi::class)
package gc_stack_roots.tier1_features.f1_root_types

import kotlin.native.runtime.GC

class Node(val value: Int, var next: Node? = null)

fun box(): String {
    val a = Node(1)
    val b = Node(2)
    val c = Node(3)
    a.next = b
    b.next = c

    GC.collect()

    if (a.value != 1 || b.value != 2 || c.value != 3) {
        return "FAIL: root values corrupted after GC"
    }
    if (a.next?.value != 2 || a.next?.next?.value != 3) {
        return "FAIL: reference links corrupted"
    }
    return "OK"
}
