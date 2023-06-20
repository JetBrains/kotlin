// ORIGINAL: /compiler/testData/diagnostics/testsWithJsStdLib/jsCode/deleteOperation.fir.kt
// WITH_STDLIB
// !DIAGNOSTICS: -UNUSED_PARAMETER

fun foo(x: Any) {
    js("delete x.foo;")
    js("delete x['bar'];")
    js("delete x.baz();")
    js("delete this;")
}


fun box() = "OK"
