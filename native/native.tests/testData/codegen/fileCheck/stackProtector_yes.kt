// TARGET_BACKEND: NATIVE
// DISABLE_NATIVE: targetFamily=MINGW
// FILECHECK_STAGE: StackProtectorPhase
// FREE_COMPILER_ARGS: -Xbinary=stackProtector=YES

// CHECK: Function Attrs: ssp{{[[:space:]].*}}define ptr{{( addrspace\(1\))?}} @"kfun:#box(){}kotlin.String"
fun box() = "OK"