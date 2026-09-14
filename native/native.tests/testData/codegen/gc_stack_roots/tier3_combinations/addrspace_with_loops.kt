// TARGET_BACKEND: NATIVE
// FREE_COMPILER_ARGS: -Xbinary=lateShadowStack=true
@file:OptIn(kotlin.native.runtime.NativeRuntimeApi::class)
package gc_stack_roots.tier3_combinations

import kotlin.native.runtime.GC

class LoopAccumulator(var sum: Long)

fun box(): String {
    val acc = LoopAccumulator(0L)
    val list = ArrayList<LoopAccumulator>()

    for (i in 1..20) {
        val step = LoopAccumulator(i.toLong())
        acc.sum += step.sum
        list.add(step)
        if (i % 5 == 0) {
            GC.collect()
        }
    }

    if (acc.sum != 210L || list.size != 20) {
        return "FAIL addrspace with loops: acc=${acc.sum}, size=${list.size}"
    }
    return "OK"
}
