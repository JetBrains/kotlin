// TARGET_BACKEND: NATIVE
// FREE_COMPILER_ARGS: -Xbinary=lateShadowStack=true
package gc_stack_roots.tier1_features.f2_leaf_lowering

object Constants {
    const val A = 100
    const val B = 200
}

class Holder {
    companion object {
        const val C = 300
    }
}

fun readConstants(): Int {
    return Constants.A + Constants.B + Holder.C
}

fun box(): String {
    val res = readConstants()
    if (res != 600) return "FAIL: expected 600, got $res"
    return "OK"
}
