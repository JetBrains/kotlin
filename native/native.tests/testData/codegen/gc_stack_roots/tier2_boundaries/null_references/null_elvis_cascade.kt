// TARGET_BACKEND: NATIVE
// FREE_COMPILER_ARGS: -Xbinary=lateShadowStack=true
@file:OptIn(kotlin.native.runtime.NativeRuntimeApi::class)
package gc_stack_roots.tier2_boundaries.null_references

import kotlin.native.runtime.GC

class Fallback(val msg: String)

fun cascade(a: String?, b: String?, c: String?): String {
    GC.collect()
    val res = a ?: b ?: c ?: Fallback("ultimate_fallback").msg
    return res
}

fun box(): String {
    val r1 = cascade(null, null, null)
    val r2 = cascade(null, "second", null)
    val r3 = cascade("first", null, null)

    if (r1 != "ultimate_fallback") return "FAIL r1: $r1"
    if (r2 != "second") return "FAIL r2: $r2"
    if (r3 != "first") return "FAIL r3: $r3"

    return "OK"
}
