// TARGET_BACKEND: NATIVE
// FREE_COMPILER_ARGS: -Xbinary=lateShadowStack=true
@file:OptIn(kotlin.native.runtime.NativeRuntimeApi::class)
package gc_stack_roots.tier3_combinations

import kotlin.native.runtime.GC

class ScopeOne(val v: Int)
class ScopeTwo(val s: String)

fun recursiveSlotSharing(depth: Int): Int {
    if (depth <= 0) {
        GC.collect()
        return 0
    }

    val first = run {
        val s1 = ScopeOne(depth)
        s1.v
    }

    val second = run {
        val s2 = ScopeTwo("depth_$depth")
        s2.s.length
    }

    val child = recursiveSlotSharing(depth - 1)
    return first + second + child
}

fun box(): String {
    val result = recursiveSlotSharing(10)
    if (result != 126) return "FAIL slot sharing with recursion: $result"
    return "OK"
}
