// TARGET_BACKEND: NATIVE
// FILECHECK_STAGE: CStubs
// FREE_COMPILER_ARGS: -Xbinary=preCodegenInlineThreshold=40
// FREE_COMPILER_ARGS: -opt-in=kotlin.experimental.ExperimentalNativeApi
import kotlin.native.NoInline

// CHECK-OPT-NOT: define ptr @"kfun:#<get-foo>(){}kotlin.String"
val foo: String
    get() { return "O" }

// CHECK: define ptr @"kfun:#<get-bar>(){}kotlin.String"
@NoInline
val bar: String
    get() { return "K" }

// CHECK-LABEL: define ptr @"kfun:#box(){}kotlin.String"
@NoInline
fun box(): String {
    // CHECK-NOT: {call|invoke} ptr @"kfun:#<get-foo>(){}kotlin.String"
    // CHECK: call ptr @"kfun:#<get-bar>(){}kotlin.String"
    return foo + bar
}
