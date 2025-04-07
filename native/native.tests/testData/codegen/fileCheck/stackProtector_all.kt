// TARGET_BACKEND: NATIVE
// FILECHECK_STAGE: StackProtectorPhase
// FREE_COMPILER_ARGS: -Xbinary=stackProtector=ALL

// CHECK: Function Attrs: sspreq{{[[:space:]].*}}define ptr @"kfun:#box(){}kotlin.String"
fun box() = "OK"