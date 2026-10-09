// TARGET_BACKEND: NATIVE
// FREE_COMPILER_ARGS: -Xbinary=lateShadowStack=true
@file:OptIn(kotlin.native.runtime.NativeRuntimeApi::class)
package gc_stack_roots.tier1_features.f6_safepoints_alloc

import kotlin.native.runtime.GC

class Chunk(val index: Int, val payload: ByteArray)

fun rapidAllocations(count: Int): Int {
    var retained: Chunk? = null
    for (i in 0 until count) {
        val temp = Chunk(i, ByteArray(128))
        if (i == count - 1) {
            retained = temp
        }
    }
    GC.collect()
    return retained?.index ?: -1
}

fun box(): String {
    val res = rapidAllocations(1000)
    if (res != 999) return "FAIL rapidAllocations: $res"
    return "OK"
}
