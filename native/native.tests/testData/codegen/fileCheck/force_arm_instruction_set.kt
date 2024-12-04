// TARGET_BACKEND: NATIVE

// Perform check after optimization pipeline because
// LLVM might infer attributes.
// FILECHECK_STAGE: LTOBitcodeOptimization

// NB: This is not a good test.
// What we actually want to check is that Kotlin/Native compiler is able to produce "big"
// binaries for 32-bit Apple Watch target. The problem is that such test is very-very-very
// slow to compile (~20 min on a local machine) which is not acceptable for CI.
//
// Instead, here we check that none of the module's functions are compiled in the Thumb mode.
// BL instructions in thumb mode have smaller ranges which causes "relocation out of range" failure.

// CHECK-WATCHOS_ARM32: target triple = "armv7k-apple-watchos2{{.+}}"

fun box() = "OK"

// CHECK-NOT: attributes #{{[0-9]+}} = { {{.+}}+thumb-mode{{.+}} }