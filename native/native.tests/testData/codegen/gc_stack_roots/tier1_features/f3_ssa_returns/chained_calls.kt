// TARGET_BACKEND: NATIVE
// FREE_COMPILER_ARGS: -Xbinary=lateShadowStack=true
@file:OptIn(kotlin.native.runtime.NativeRuntimeApi::class)
package gc_stack_roots.tier1_features.f3_ssa_returns

import kotlin.native.runtime.GC

class Builder(val value: String) {
    fun append(s: String): Builder {
        return Builder(this.value + s)
    }
}

fun box(): String {
    val b = Builder("A")
        .append("B")
        .append("C")
        .append("D")

    GC.collect()

    if (b.value != "ABCD") return "FAIL chained: ${b.value}"
    return "OK"
}
