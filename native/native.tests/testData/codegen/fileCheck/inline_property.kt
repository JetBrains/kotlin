// TARGET_BACKEND: NATIVE
// FILECHECK_STAGE: CStubs
// FREE_COMPILER_ARGS: -Xbinary=preCodegenInlineThreshold=40
// FREE_COMPILER_ARGS: -opt-in=kotlin.experimental.ExperimentalNativeApi
// IGNORE_NATIVE: optimizationMode=OPT && cacheMode=STATIC_ONLY_DIST
// IGNORE_NATIVE: optimizationMode=OPT && cacheMode=STATIC_EVERYWHERE
import kotlin.native.NoInline

// CHECK-OPT-NOT: define {{.*}}@"kfun:#<get-foo>(){}kotlin.String"
val foo: String
    get() { return "O" }

// CHECK: define ptr @"kfun:#<get-bar>(){}kotlin.String"
@NoInline
val bar: String
    get() { return "K" }

// CHECK-LABEL: define ptr @"kfun:#box(){}kotlin.String"
@NoInline
fun box(): String {
    // CHECK-OPT-NOT: {{call|invoke}} {{.*}}@"kfun:#<get-foo>(){}kotlin.String"
    // CHECK: call ptr @"kfun:#<get-bar>(){}kotlin.String"
    return foo + bar
}
