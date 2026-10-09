// TARGET_BACKEND: NATIVE
// FREE_COMPILER_ARGS: -Xbinary=lateShadowStack=true
@file:OptIn(kotlin.native.runtime.NativeRuntimeApi::class)
package gc_stack_roots.tier1_features.f3_ssa_returns

import kotlin.native.runtime.GC

data class Wrapper<T>(val item: T)

fun <T> wrap(item: T): Wrapper<T> {
    return Wrapper(item)
}

fun box(): String {
    val w1 = wrap("Kotlin")
    val w2 = wrap(2026)

    GC.collect()

    if (w1.item != "Kotlin" || w2.item != 2026) {
        return "FAIL generic return: w1=$w1, w2=$w2"
    }
    return "OK"
}
