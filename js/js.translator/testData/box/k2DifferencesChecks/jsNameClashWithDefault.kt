// ORIGINAL: /compiler/testData/diagnostics/testsWithJsStdLib/name/jsNameClashWithDefault.fir.kt
// WITH_STDLIB
package foo

@JsName("bar") fun foo(x: Int) = x

fun bar() = 42


fun box() = "OK"
