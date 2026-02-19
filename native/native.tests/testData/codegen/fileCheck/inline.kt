// TARGET_BACKEND: NATIVE
// FILECHECK_STAGE: CStubs
// FREE_COMPILER_ARGS: -Xbinary=preCodegenInlineThreshold=40
// FREE_COMPILER_ARGS: -opt-in=kotlin.experimental.ExperimentalNativeApi
import kotlin.native.NoInline

// CHECK-OPT-NOT: define ptr @"kfun:#foo(){}kotlin.String"
fun foo(): String {
    return "O"
}

// CHECK: define ptr @"kfun:#bar(){}kotlin.String"
@NoInline
fun bar(): String {
    return "K"
}

// CHECK-LABEL: define ptr @"kfun:#box(){}kotlin.String"
@NoInline
fun box(): String {
    // CHECK-NOT: {call|invoke} ptr @"kfun:#foo(){}kotlin.String"
    // CHECK: call ptr @"kfun:#bar(){}kotlin.String"
    return foo() + bar()
}
