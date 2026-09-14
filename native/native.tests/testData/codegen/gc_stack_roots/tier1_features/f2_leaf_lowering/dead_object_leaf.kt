// TARGET_BACKEND: NATIVE
// FREE_COMPILER_ARGS: -Xbinary=lateShadowStack=true
package gc_stack_roots.tier1_features.f2_leaf_lowering

class Dummy(val num: Int)

fun extractPrimitive(): Int {
    val d = Dummy(99)
    return d.num + 1
}

fun box(): String {
    val res = extractPrimitive()
    if (res != 100) return "FAIL extractPrimitive: $res"
    return "OK"
}
