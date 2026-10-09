// TARGET_BACKEND: NATIVE
// FREE_COMPILER_ARGS: -Xbinary=lateShadowStack=true
@file:OptIn(kotlin.native.runtime.NativeRuntimeApi::class)
package gc_stack_roots.tier1_features.f11_addrspace_lowering

import kotlin.native.runtime.GC

class Item(val id: Int)

val sink = arrayOfNulls<Item>(2)

private fun churn(): Int {
    var s = 0
    for (i in 0 until 100_000) s += Item(i).id and 1
    return s
}

fun box(): String {
    val first = Item(1)
    val second = Item(2)
    GC.collect()
    churn()
    sink[0] = first
    sink[1] = second
    if (sink[0]!!.id != 1) return "FAIL first: ${sink[0]!!.id}"
    if (sink[1]!!.id != 2) return "FAIL second: ${sink[1]!!.id}"
    return "OK"
}
