// EXPECTED_REACHABLE_NODES: 496
package foo

class A<T>(val a: T) {
    val foo = { a }
}

fun <T> T.bar() = { this }

fun box(): String {
    assertEquals("ok", A("ok").foo())
    assertEquals("a42", "a42".bar()())

    return "OK"
}
