// TARGET_BACKEND: NATIVE
// FREE_COMPILER_ARGS: -Xbinary=lateShadowStack=true
package gc_stack_roots.tier1_features.f2_leaf_lowering

fun complexMath(x: Double, y: Double): Double {
    var acc = x
    for (i in 0 until 10) {
        acc = (acc + y) * 0.5
    }
    return acc
}

fun box(): String {
    val res = complexMath(100.0, 10.0)
    if (res < 10.0 || res > 100.0) return "FAIL math: $res"
    return "OK"
}
