// ORIGINAL: /compiler/testData/diagnostics/testsWithJsStdLib/name/extensionPropertyAndMethod.fir.kt
// WITH_STDLIB
package foo

class A

fun A.get_bar() = 23

val A.bar: Int
  get() = 42


fun box() = "OK"
