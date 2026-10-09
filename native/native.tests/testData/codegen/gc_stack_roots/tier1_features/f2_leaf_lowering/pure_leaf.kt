// TARGET_BACKEND: NATIVE
// FREE_COMPILER_ARGS: -Xbinary=lateShadowStack=true
package gc_stack_roots.tier1_features.f2_leaf_lowering

fun pureLeafAdd(x: Int, y: Int): Int {
    return x + y
}

fun box(): String {
    val res = pureLeafAdd(40, 2)
    if (res != 42) return "FAIL: expected 42, got $res"
    return "OK"
}
