package foo

class A<T>(val a: T) {
    val foo = { a }
}

fun <T> T.bar() = { this }

fun assertEquals(expected: String, actual: String) {
    if (expected != actual) throw Exception("Expected: $expected, actual: $actual")
}

fun box(): String {
    assertEquals("ok", A("ok").foo())
    assertEquals("a42", "a42".bar()())

    return "OK"
}
