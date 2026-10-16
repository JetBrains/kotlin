// TARGET_BACKEND: NATIVE
// FREE_COMPILER_ARGS: -Xbinary=lateShadowStack=true
@file:OptIn(kotlin.native.runtime.NativeRuntimeApi::class)
package gc_stack_roots.tier1_features.f10_exception_unwind

import kotlin.native.runtime.GC

class ErrorContext(val tag: String)

fun testTryCatchRoots(throwError: Boolean): String {
    val ctx = ErrorContext("valid_context")
    try {
        if (throwError) {
            throw RuntimeException("Intentional failure")
        }
        return "normal_${ctx.tag}"
    } catch (e: RuntimeException) {
        GC.collect()
        return "caught_${ctx.tag}"
    }
}

fun box(): String {
    val rNormal = testTryCatchRoots(false)
    val rCaught = testTryCatchRoots(true)

    if (rNormal != "normal_valid_context") return "FAIL normal: $rNormal"
    if (rCaught != "caught_valid_context") return "FAIL caught: $rCaught"

    return "OK"
}
