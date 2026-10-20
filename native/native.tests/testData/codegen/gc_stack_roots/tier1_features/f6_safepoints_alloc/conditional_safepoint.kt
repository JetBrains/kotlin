// TARGET_BACKEND: NATIVE
// FREE_COMPILER_ARGS: -Xbinary=lateShadowStack=true
@file:OptIn(kotlin.native.runtime.NativeRuntimeApi::class)
package gc_stack_roots.tier1_features.f6_safepoints_alloc

import kotlin.native.runtime.GC

class PathObj(val pathId: Int)

fun conditionalSafepoint(flag: Boolean): PathObj {
    val obj = PathObj(if (flag) 100 else 200)
    if (flag) {
        GC.collect()
    }
    return obj
}

fun box(): String {
    val o1 = conditionalSafepoint(true)
    val o2 = conditionalSafepoint(false)

    if (o1.pathId != 100) return "FAIL o1: ${o1.pathId}"
    if (o2.pathId != 200) return "FAIL o2: ${o2.pathId}"

    return "OK"
}
