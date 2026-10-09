// TARGET_BACKEND: NATIVE
// FREE_COMPILER_ARGS: -Xbinary=lateShadowStack=true
@file:OptIn(kotlin.native.runtime.NativeRuntimeApi::class)
package gc_stack_roots.tier3_combinations

import kotlin.native.runtime.GC

class InlineRoot(val tag: String)

inline fun runInlinedAction(action: () -> Unit) {
    action()
}

fun box(): String {
    val root = InlineRoot("outer_frame_root")
    var caught = false

    try {
        runInlinedAction {
            val innerRoot = InlineRoot("inner_inline_root")
            GC.collect()
            throw IllegalStateException("error_from_inline_${innerRoot.tag}")
        }
    } catch (e: IllegalStateException) {
        GC.collect()
        caught = true
        if (e.message != "error_from_inline_inner_inline_root") {
            return "FAIL exception message: ${e.message}"
        }
    }

    if (!caught || root.tag != "outer_frame_root") {
        return "FAIL inlining with exceptions: caught=$caught, root=${root.tag}"
    }
    return "OK"
}
