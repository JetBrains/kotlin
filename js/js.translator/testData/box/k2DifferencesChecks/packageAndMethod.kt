// ORIGINAL: /compiler/testData/diagnostics/testsWithJsStdLib/name/packageAndMethod.fir.kt
// WITH_STDLIB
// FILE: foo.kt

package foo

fun bar() = 23

// FILE: foobar.kt

package foo.bar

val x = 42


fun box() = "OK"
