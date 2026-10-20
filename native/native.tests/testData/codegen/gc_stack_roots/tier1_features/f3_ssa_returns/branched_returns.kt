// TARGET_BACKEND: NATIVE
// FREE_COMPILER_ARGS: -Xbinary=lateShadowStack=true
@file:OptIn(kotlin.native.runtime.NativeRuntimeApi::class)
package gc_stack_roots.tier1_features.f3_ssa_returns

import kotlin.native.runtime.GC

class Message(val text: String)

fun pickBranch(flag: Boolean): Message {
    return if (flag) {
        Message("true_branch")
    } else {
        Message("false_branch")
    }
}

fun box(): String {
    val m1 = pickBranch(true)
    val m2 = pickBranch(false)

    GC.collect()

    if (m1.text != "true_branch") return "FAIL m1"
    if (m2.text != "false_branch") return "FAIL m2"
    return "OK"
}
