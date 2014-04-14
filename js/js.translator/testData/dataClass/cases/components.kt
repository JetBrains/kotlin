package foo

data class D(val start: String, middle: String, val end: String) {
    fun getLabel() : String {
        return start + end
    }
}

fun assertEquals<T>(expected: T, actual: T) {
    if (expected != actual) throw Exception("expected: $expected, actual, $actual")
}

fun assertNotEqual<T>(expected: T, actual: T) {
    if (expected == actual) throw Exception("unexpectedly equal: $expected and $actual")
}

fun box(): String {
    val d = D("max", "-", "min")
    assertEquals("maxmin", d.getLabel())
    val (p1, p2) = d
    assertEquals("max", p1)
    assertEquals("min", p2)
    return "OK"
}