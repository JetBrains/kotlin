// ORIGINAL: /compiler/testData/diagnostics/testsWithJsStdLib/name/packageAndProperty.fir.kt
// WITH_STDLIB
// FILE: foo.kt

package foo

val bar = 23

// FILE: foobar.kt

package foo.bar

val x = 42


fun box() = "OK"
