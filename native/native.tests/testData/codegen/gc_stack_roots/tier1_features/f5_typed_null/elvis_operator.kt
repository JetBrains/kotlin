// TARGET_BACKEND: NATIVE
// FREE_COMPILER_ARGS: -Xbinary=lateShadowStack=true
@file:OptIn(kotlin.native.runtime.NativeRuntimeApi::class)
package gc_stack_roots.tier1_features.f5_typed_null

import kotlin.native.runtime.GC

class ValuePayload(val info: String)

fun resolveInfo(primary: ValuePayload?, fallback: ValuePayload): String {
    GC.collect()
    val chosen = primary ?: fallback
    return chosen.info
}

fun box(): String {
    val fallback = ValuePayload("default_info")
    val p1 = ValuePayload("custom_info")

    val res1 = resolveInfo(p1, fallback)
    val res2 = resolveInfo(null, fallback)

    if (res1 != "custom_info") return "FAIL res1: $res1"
    if (res2 != "default_info") return "FAIL res2: $res2"

    return "OK"
}
