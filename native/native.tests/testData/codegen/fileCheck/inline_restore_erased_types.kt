// TARGET_BACKEND: NATIVE
// FILECHECK_STAGE: CStubs

// CHECK-LABEL: define ptr @"kfun:#foo(kotlin.Int){}kotlin.String"
// CHECK-OPT-NOT
// CHECK-DEBUG: Int-box
// CHECK-LABEL: epilogue:
fun foo(x: Int) = x.toString()

// CHECK-LABEL: define ptr @"kfun:#bar(kotlin.Int){}kotlin.String"
// CHECK-NOT: Int-box
// CHECK-NOT: Int-unbox
// CHECK-LABEL: epilogue:
fun bar(y: Int): String {
    val s = y.let { foo(it) }
    return s
}

// CHECK-LABEL: define ptr @"kfun:#box(){}kotlin.String"
// CHECK-NOT: Int-box
// CHECK-NOT: Int-unbox
// CHECK-LABEL: epilogue:
fun box(): String {
    val s = bar(42)
    return if (s == "42") "OK" else "fail: $s"
}
