// TARGET_BACKEND: NATIVE
// FREE_COMPILER_ARGS: -Xbinary=lateShadowStack=true
@file:OptIn(kotlin.native.runtime.NativeRuntimeApi::class)
package gc_stack_roots.tier1_features.f8_slot_sharing

import kotlin.native.runtime.GC

class LeftBranch(val info: String)
class RightBranch(val code: Int)

fun executeBranch(cond: Boolean): String {
    return if (cond) {
        val left = LeftBranch("left_branch_active")
        GC.collect()
        left.info
    } else {
        val right = RightBranch(12345)
        GC.collect()
        "right_${right.code}"
    }
}

fun box(): String {
    val r1 = executeBranch(true)
    val r2 = executeBranch(false)

    if (r1 != "left_branch_active") return "FAIL r1: $r1"
    if (r2 != "right_12345") return "FAIL r2: $r2"

    return "OK"
}
