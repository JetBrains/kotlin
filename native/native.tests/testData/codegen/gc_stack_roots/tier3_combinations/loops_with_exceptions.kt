// TARGET_BACKEND: NATIVE
// FREE_COMPILER_ARGS: -Xbinary=lateShadowStack=true
@file:OptIn(kotlin.native.runtime.NativeRuntimeApi::class)
package gc_stack_roots.tier3_combinations

import kotlin.native.runtime.GC

class IterationState(val iter: Int)

fun box(): String {
    var caughtCount = 0
    var validSum = 0

    for (i in 0 until 20) {
        val state = IterationState(i)
        try {
            if (i % 3 == 0) {
                GC.collect()
                throw IllegalArgumentException("mod 3 fail")
            }
            validSum += state.iter
        } catch (e: IllegalArgumentException) {
            caughtCount++
            GC.collect()
        }
    }

    if (caughtCount != 7 || validSum != 127) {
        return "FAIL loops with exceptions: caught=$caughtCount, validSum=$validSum"
    }
    return "OK"
}
