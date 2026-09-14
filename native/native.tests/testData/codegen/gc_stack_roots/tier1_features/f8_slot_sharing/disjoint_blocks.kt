// TARGET_BACKEND: NATIVE
// FREE_COMPILER_ARGS: -Xbinary=lateShadowStack=true
@file:OptIn(kotlin.native.runtime.NativeRuntimeApi::class)
package gc_stack_roots.tier1_features.f8_slot_sharing

import kotlin.native.runtime.GC

class ScopeA(val name: String)
class ScopeB(val value: Int)

fun testDisjointScopes(): Pair<String, Int> {
    val resultA = run {
        val a = ScopeA("first_scope")
        GC.collect()
        a.name
    }

    val resultB = run {
        val b = ScopeB(999)
        GC.collect()
        b.value
    }

    return Pair(resultA, resultB)
}

fun box(): String {
    val (s, v) = testDisjointScopes()
    if (s != "first_scope" || v != 999) {
        return "FAIL disjoint scopes: s=$s, v=$v"
    }
    return "OK"
}
