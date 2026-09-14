// TARGET_BACKEND: NATIVE
// FREE_COMPILER_ARGS: -Xbinary=lateShadowStack=true
@file:OptIn(kotlin.native.runtime.NativeRuntimeApi::class)
package gc_stack_roots.tier1_features.f8_slot_sharing

import kotlin.native.runtime.GC

class TypeAlpha(val x: Double)
class TypeBeta(val y: Long)
class TypeGamma(val z: String)

fun runHeterogeneousScopes(): String {
    val aRes = run {
        val a = TypeAlpha(3.14159)
        GC.collect()
        "a:${a.x}"
    }

    val bRes = run {
        val b = TypeBeta(9876543210L)
        GC.collect()
        "b:${b.y}"
    }

    val cRes = run {
        val c = TypeGamma("gamma_string")
        GC.collect()
        "c:${c.z}"
    }

    return "$aRes;$bRes;$cRes"
}

fun box(): String {
    val result = runHeterogeneousScopes()
    if (!result.startsWith("a:3.14159;b:9876543210;c:gamma_string")) {
        return "FAIL heterogeneous sharing: $result"
    }
    return "OK"
}
