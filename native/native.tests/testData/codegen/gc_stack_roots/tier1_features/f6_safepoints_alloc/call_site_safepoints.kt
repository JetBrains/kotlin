// TARGET_BACKEND: NATIVE
// FREE_COMPILER_ARGS: -Xbinary=lateShadowStack=true
@file:OptIn(kotlin.experimental.ExperimentalNativeApi::class, kotlin.native.runtime.NativeRuntimeApi::class)
package gc_stack_roots.tier1_features.f6_safepoints_alloc

import kotlin.native.runtime.GC

class CallerRecord(val id: Int, var data: String)

fun externalWorker(rec: CallerRecord): String {
    GC.collect()
    return "processed_${rec.id}_${rec.data}"
}

fun runCallSites(): String {
    val r1 = CallerRecord(1, "alpha")
    val r2 = CallerRecord(2, "beta")

    val s1 = externalWorker(r1)
    val s2 = externalWorker(r2)

    return "$s1;$s2"
}

fun box(): String {
    val result = runCallSites()
    if (result != "processed_1_alpha;processed_2_beta") {
        return "FAIL call sites: $result"
    }
    return "OK"
}
