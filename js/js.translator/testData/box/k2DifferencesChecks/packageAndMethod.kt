// ORIGINAL: /compiler/testData/diagnostics/testsWithJsStdLib/name/packageAndMethod.fir.kt
// WITH_STDLIB
// FIR_DIFFERENCE
// This case can't be checked using FIR. It is checked later on klib serialization.

// FILE: foo.kt

package foo

fun bar() = 23

// FILE: foobar.kt

package foo.bar

val x = 42


fun box() = "OK"
