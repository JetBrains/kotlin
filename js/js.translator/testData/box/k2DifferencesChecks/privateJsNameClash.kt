// ORIGINAL: /compiler/testData/diagnostics/testsWithJsStdLib/name/privateJsNameClash.fir.kt
// WITH_STDLIB
package foo

@JsName("bar") private fun foo(x: Int) = x

fun bar() = 42


fun box() = "OK"
