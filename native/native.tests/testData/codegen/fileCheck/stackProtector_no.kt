// TARGET_BACKEND: NATIVE
// FILECHECK_STAGE: StackProtectorPhase
// FREE_COMPILER_ARGS: -Xbinary=stackProtector=NO

// CHECK-NOT: {{ssp|sspreq|sspstrong}}
fun box() = "OK"