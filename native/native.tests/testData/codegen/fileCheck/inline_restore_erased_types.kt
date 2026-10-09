// TARGET_BACKEND: NATIVE
// FILECHECK_STAGE: CStubs

// CHECK-EAGER_SHADOW_STACK-LABEL: define ptr @"kfun:#foo(kotlin.Int){}kotlin.String"
// CHECK-LATE_SHADOW_STACK-LABEL: define ptr addrspace(1) @"kfun:#foo(kotlin.Int){}kotlin.String"
// CHECK-OPT-NOT
// CHECK-DEBUG: Int-box
// CHECK-LABEL: epilogue:
fun foo(x: Int) = x.toString()

// CHECK-EAGER_SHADOW_STACK-LABEL: define ptr @"kfun:#bar(kotlin.Int){}kotlin.String"
// CHECK-LATE_SHADOW_STACK-LABEL: define ptr addrspace(1) @"kfun:#bar(kotlin.Int){}kotlin.String"
// CHECK-NOT: Int-box
// CHECK-NOT: Int-unbox
// CHECK-LABEL: epilogue:
fun bar(y: Int): String {
    val s = y.let { foo(it) }
    return s
}

// CHECK-EAGER_SHADOW_STACK-LABEL: define ptr @"kfun:#box(){}kotlin.String"
// CHECK-LATE_SHADOW_STACK-LABEL: define ptr addrspace(1) @"kfun:#box(){}kotlin.String"
// CHECK-NOT: Int-box
// CHECK-NOT: Int-unbox
// CHECK-LABEL: epilogue:
fun box(): String {
    val s = bar(42)
    return if (s == "42") "OK" else "fail: $s"
}
