// IGNORE_BACKEND: JS_IR
// EXPECTED_REACHABLE_NODES: 1110
package test

external fun foo(ignore: dynamic): String

@JsName("foo")
fun foo() = "K"

fun box() = foo(0) + foo()