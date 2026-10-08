// TARGET_BACKEND: NATIVE
// FILECHECK_STAGE: BuildShadowStack
// FREE_COMPILER_ARGS: -Xbinary=lateShadowStack=true

@file:Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER")

import kotlin.native.Retain

object A {
    const val x = 5
}

class B(val z: Int) {
    companion object {
        const val y = 7
    }
}

object C {
    val x = listOf(1, 2, 3)
}

// CHECK-LABEL: define {{.*}}i32 @"kfun:#f(kotlin.Int){}kotlin.Int"(i32 %0)
// CHECK-NOT: EnterFrame
@Retain
fun f(n: Int) = A.x + B.y + n

// CHECK-LABEL: define {{.*}}i32 @"kfun:#g(kotlin.Int){}kotlin.Int"(i32 %0)
// CHECK: EnterFrame
@Retain
fun g(n: Int) = C.x[n]

fun box(): String {
    val fRes = f(0)
    if (fRes != 12) return "FAIL: f() = $fRes != 12"

    val gRes = g(1)
    if (gRes != 2) return "FAIL: g() = $gRes != 2"

    return "OK"
}
