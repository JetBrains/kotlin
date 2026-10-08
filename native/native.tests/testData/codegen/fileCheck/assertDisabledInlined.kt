// TARGET_BACKEND: NATIVE
// FILECHECK_STAGE: CStubs
// ASSERTIONS_MODE: always-disable
// WITH_STDLIB

@OptIn(kotlin.experimental.ExperimentalNativeApi::class)

inline fun foo(x: Boolean) = assert(x)

// CHECK-EAGER_SHADOW_STACK-LABEL: define ptr @"kfun:#box(){}kotlin.String"
// CHECK-LATE_SHADOW_STACK-LABEL: define ptr addrspace(1) @"kfun:#box(){}kotlin.String"
// CHECK-NOT: call void @"kfun:kotlin.AssertionError#<init>(kotlin.Any?){}"
fun box(): String {
    foo(false)
    return "OK"
}