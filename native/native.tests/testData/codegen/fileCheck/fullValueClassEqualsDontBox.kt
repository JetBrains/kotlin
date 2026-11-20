// ISSUE: KT-89978
// TARGET_BACKEND: NATIVE
// FILECHECK_STAGE: CStubs
// LANGUAGE: +FullValueClasses

value class Single(val x: Int)
value class Outer(val single: Single)

// CHECK-LABEL: define ptr @"kfun:#box(){}kotlin.String"
fun box(): String {
    // CHECK-LABEL: entry
    // CHECK-NOT: <Single-box>
    // CHECK-NOT: <Outer-box>
    // CHECK-NOT: Single#equals
    // CHECK-NOT: Outer#equals
    val a = Single(1)
    val b = Single(2)
    if (a == b) return "Fail 1"
    if (Outer(a) == Outer(b)) return "Fail 2"
    // CHECK-LABEL: epilogue:
    return "OK"
}
