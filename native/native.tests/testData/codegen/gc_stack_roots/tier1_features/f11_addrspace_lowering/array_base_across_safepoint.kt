// TARGET_BACKEND: NATIVE
// FREE_COMPILER_ARGS: -Xbinary=lateShadowStack=true
@file:OptIn(kotlin.native.runtime.NativeRuntimeApi::class)
package gc_stack_roots.tier1_features.f11_addrspace_lowering

import kotlin.native.runtime.GC

class Payload(val value: Int)

private fun allocateAndFill(n: Int): Long {
    val slots = arrayOfNulls<Payload>(n)
    var i = 0
    while (i < n) {
        slots[i] = Payload(i)
        if (i == n / 2) GC.collect()
        i++
    }
    var sum = 0L
    for (p in slots) sum += p!!.value
    return sum
}

fun box(): String {
    val rounds = 50
    val size = 2000
    var acc = 0L
    for (r in 0 until rounds) acc += allocateAndFill(size)

    val expected = rounds.toLong() * (size.toLong() * (size - 1) / 2)
    if (acc != expected) return "FAIL sum $acc, expected $expected"
    return "OK"
}
