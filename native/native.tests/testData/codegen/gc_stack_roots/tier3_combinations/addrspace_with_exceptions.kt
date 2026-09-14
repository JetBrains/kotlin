// TARGET_BACKEND: NATIVE
// FREE_COMPILER_ARGS: -Xbinary=lateShadowStack=true
@file:OptIn(kotlin.native.runtime.NativeRuntimeApi::class)
package gc_stack_roots.tier3_combinations

import kotlin.native.runtime.GC

class StackContext(val name: String, var status: String)

fun riskyOperation(ctx: StackContext, shouldFail: Boolean): String {
    try {
        if (shouldFail) {
            GC.collect()
            throw IllegalArgumentException("failed in riskyOperation")
        }
        ctx.status = "completed"
        return ctx.name
    } catch (e: IllegalArgumentException) {
        GC.collect()
        ctx.status = "recovered"
        return "error_caught"
    }
}

fun box(): String {
    val ctx1 = StackContext("ctx1", "initial")
    val res1 = riskyOperation(ctx1, false)
    if (res1 != "ctx1" || ctx1.status != "completed") return "FAIL res1: $res1"

    val ctx2 = StackContext("ctx2", "initial")
    val res2 = riskyOperation(ctx2, true)
    if (res2 != "error_caught" || ctx2.status != "recovered") return "FAIL res2: $res2"

    return "OK"
}
