// ORIGINAL: /compiler/testData/diagnostics/testsWithJsStdLib/jsCode/badAssignment.fir.kt
// WITH_STDLIB
// !DIAGNOSTICS: -UNUSED_PARAMETER
fun Int.foo(x: Int) {
    js("this = x;")
}


fun box() = "OK"
