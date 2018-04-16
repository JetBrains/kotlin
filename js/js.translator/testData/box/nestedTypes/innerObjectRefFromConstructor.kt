// IGNORE_BACKEND: JS_IR
// EXPECTED_REACHABLE_NODES: 1118
package foo

class X {
    val a = Y.foo

    object Y {
        val foo = 23
    }
}

fun box(): String {
    assertEquals(23, X().a)
    return "OK"
}