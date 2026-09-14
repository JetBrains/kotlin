// TARGET_BACKEND: NATIVE
// FREE_COMPILER_ARGS: -Xbinary=lateShadowStack=true
@file:OptIn(kotlin.native.runtime.NativeRuntimeApi::class)
package gc_stack_roots.tier1_features.f10_exception_unwind

import kotlin.native.runtime.GC

class CleanupTarget(var cleaned: Boolean = false)

fun unwindWithFinally(): Boolean {
    val target = CleanupTarget()
    try {
        try {
            throw IllegalStateException("Trigger unwind")
        } finally {
            GC.collect()
            target.cleaned = true
        }
    } catch (e: IllegalStateException) {
        return target.cleaned
    }
}

fun box(): String {
    val success = unwindWithFinally()
    if (!success) return "FAIL try_finally_unwind"
    return "OK"
}
