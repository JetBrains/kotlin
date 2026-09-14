// TARGET_BACKEND: NATIVE
// FREE_COMPILER_ARGS: -Xbinary=lateShadowStack=true
package gc_stack_roots.tier1_features.f2_leaf_lowering

inline fun inlineMultiply(a: Int, b: Int): Int {
    return a * b
}

inline fun inlineTransform(x: Int, op: (Int) -> Int): Int {
    return op(x)
}

fun box(): String {
    val m = inlineMultiply(6, 7)
    if (m != 42) return "FAIL inlineMultiply: $m"

    val t = inlineTransform(10) { it * 2 + 3 }
    if (t != 23) return "FAIL inlineTransform: $t"

    return "OK"
}
