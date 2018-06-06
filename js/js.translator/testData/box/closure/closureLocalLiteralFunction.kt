// IGNORE_BACKEND: JS_IR
// EXPECTED_REACHABLE_NODES: 1114
package foo

val k = { "K" }

fun test(): String {
    val o = { "O" }

    val funLit = { o() + k() }
    return funLit()
}

fun box(): String {
    return test()
}