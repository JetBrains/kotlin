// TARGET_BACKEND: NATIVE
// DISABLE_NATIVE: targetFamily=MINGW
// FILECHECK_STAGE: StackProtectorPhase
// FREE_COMPILER_ARGS: -Xbinary=stackProtector=ALL

// CHECK: Function Attrs: sspreq{{[[:space:]].*}}define ptr{{( addrspace\(1\))?}} @"kfun:#box(){}kotlin.String"
fun box() = "OK"