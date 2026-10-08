// TARGET_BACKEND: NATIVE
// FILECHECK_STAGE: CStubs
// FREE_COMPILER_ARGS: -Xbinary=preCodegenInlineThreshold=40
// FREE_COMPILER_ARGS: -opt-in=kotlin.experimental.ExperimentalNativeApi
// IGNORE_NATIVE: optimizationMode=OPT && cacheMode=STATIC_ONLY_DIST
// IGNORE_NATIVE: optimizationMode=OPT && cacheMode=STATIC_EVERYWHERE
import kotlin.native.NoInline

// CHECK-OPT-NOT: define ptr @"kfun:#foo(){}kotlin.String"
fun foo(): String {
    return "O"
}

// CHECK-EAGER_SHADOW_STACK: define ptr @"kfun:#bar(){}kotlin.String"
// CHECK-LATE_SHADOW_STACK: define ptr addrspace(1) @"kfun:#bar(){}kotlin.String"
@NoInline
fun bar(): String {
    return "K"
}

// CHECK-EAGER_SHADOW_STACK-LABEL: define ptr @"kfun:#box(){}kotlin.String"
// CHECK-LATE_SHADOW_STACK-LABEL: define ptr addrspace(1) @"kfun:#box(){}kotlin.String"
@NoInline
fun box(): String {
    // CHECK-NOT: {call|invoke} ptr @"kfun:#foo(){}kotlin.String"
    // CHECK-EAGER_SHADOW_STACK: call ptr @"kfun:#bar(){}kotlin.String"
    // CHECK-LATE_SHADOW_STACK: call ptr addrspace(1) @"kfun:#bar(){}kotlin.String"
    return foo() + bar()
}
