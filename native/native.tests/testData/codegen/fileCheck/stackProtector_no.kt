// TARGET_BACKEND: NATIVE
// FILECHECK_STAGE: StackProtectorPhase

// CHECK-NOT: {{ssp|sspreq|sspstrong}}
fun box() = "OK"