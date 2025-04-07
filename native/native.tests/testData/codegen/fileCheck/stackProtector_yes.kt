// TARGET_BACKEND: NATIVE
// FILECHECK_STAGE: StackProtectorPhase
// FREE_COMPILER_ARGS: -Xbinary=stackProtector=YES

// CHECK: Function Attrs: ssp{{[[:space:]].*}}define ptr @"kfun:#box(){}kotlin.String"
fun box() = "OK"