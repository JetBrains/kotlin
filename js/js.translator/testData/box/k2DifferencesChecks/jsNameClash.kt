// ORIGINAL: /compiler/testData/diagnostics/testsWithJsStdLib/name/jsNameClash.fir.kt
// WITH_STDLIB
package foo

@JsName("x") fun foo(x: Int) = x

@JsName("x") fun bar() = 42


fun box() = "OK"
