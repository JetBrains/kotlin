// TARGET_BACKEND: NATIVE
// FREE_COMPILER_ARGS: -Xbinary=lateShadowStack=true
@file:OptIn(kotlin.native.runtime.NativeRuntimeApi::class)
package gc_stack_roots.tier2_boundaries.null_references

import kotlin.native.runtime.GC

class ValueToken(val id: Int)

fun box(): String {
    var token: ValueToken? = null

    for (i in 0 until 100) {
        if (i % 2 == 0) {
            token = ValueToken(i)
        } else {
            token = null
        }
        if (i % 20 == 0) {
            GC.collect()
        }
    }

    if (token != null) return "FAIL token should be null after 100 iterations"
    return "OK"
}
