// TARGET_BACKEND: NATIVE
// FREE_COMPILER_ARGS: -Xbinary=lateShadowStack=true
@file:OptIn(kotlin.native.runtime.NativeRuntimeApi::class)
package gc_stack_roots.tier1_features.f3_ssa_returns

import kotlin.native.runtime.GC

class Response(val code: Int)

fun getFactory(multiplier: Int): (Int) -> Response {
    return { num ->
        Response(num * multiplier)
    }
}

fun box(): String {
    val factory = getFactory(10)
    val r1 = factory(3)
    val r2 = factory(7)

    GC.collect()

    if (r1.code != 30 || r2.code != 70) {
        return "FAIL higher order return: r1=${r1.code}, r2=${r2.code}"
    }
    return "OK"
}
