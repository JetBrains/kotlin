// EXPECTED_REACHABLE_NODES: 1284
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