// TARGET_BACKEND: NATIVE
// IGNORE_BACKEND: NATIVE
// FILECHECK_STAGE: CStubs

// The check below is intentionally wrong and must fail.
// Test system should report test as passed due to IGNORE_BACKEND directive above.
// CHECK-LABEL: NONEXISTENT
fun box(): String = "OK"
