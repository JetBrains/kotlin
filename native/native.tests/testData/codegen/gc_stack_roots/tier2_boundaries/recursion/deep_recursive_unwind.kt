// TARGET_BACKEND: NATIVE
// FREE_COMPILER_ARGS: -Xbinary=lateShadowStack=true
@file:OptIn(kotlin.native.runtime.NativeRuntimeApi::class)
package gc_stack_roots.tier2_boundaries.recursion

import kotlin.native.runtime.GC

class RecUnwindData(val level: Int)

fun recursiveThrow(level: Int): Nothing {
    val d = RecUnwindData(level)
    if (level == 25) {
        GC.collect()
        throw IllegalStateException("reached_max_level_${d.level}")
    }
    recursiveThrow(level + 1)
}

fun box(): String {
    try {
        recursiveThrow(1)
    } catch (e: IllegalStateException) {
        GC.collect()
        if (e.message != "reached_max_level_25") return "FAIL recursive unwind: ${e.message}"
        return "OK"
    }
    return "FAIL expected exception"
}
