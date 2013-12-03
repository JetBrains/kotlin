package foo

class Fail(val message: String) : RuntimeException(message) {
    val isFail = true // workaround for exception handling
}

fun assertEquals(actual: Int, expected: Int, message: String) =
        if (actual != expected) throw Fail("$message")

fun testChar(s: Char) {
    val any: Any = s
    assertEquals(s.hashCode(), any.hashCode(), "testChar failed: ${s.hashCode()} != ${any.hashCode()}")
}

fun testString(s: String) {
    val any: Any = s
    assertEquals(s.hashCode(), any.hashCode(), "testString failed: ${s.hashCode()} != ${any.hashCode()}")
}

fun testNumber(number: Number) {
    val any: Any = number
    assertEquals(number.hashCode(), any.hashCode(), "testNumber failed: ${number.hashCode()} != ${any.hashCode()}")
}

fun testInt(number: Int) {
    val any: Any = number
    assertEquals(number.hashCode(), any.hashCode(), "testInt failed: ${number.hashCode()} != ${any.hashCode()}")
}

fun box(): Boolean {
    testChar('a')
    testString("hello world")
    testInt(42)
    testNumber(42)
    testNumber(3.14159)
    testNumber(0xdeadbeef)
    return true
}