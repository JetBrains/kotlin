package foo

data class Holder<T>(val v: T)

data class D(val start: String, val end: String)

class O(val start: String, val end: String)

fun assertEquals<T>(expected: T, actual: T) {
    if (expected != actual) throw Exception("expected: $expected, actual, $actual")
}

fun assertNotEqual<T>(expected: T, actual: T) {
    if (expected == actual) throw Exception("unexpectedly equal: $expected and $actual")
}

fun box(): String {
    val d1 = D("a", "b")
    val d2 = D("a", "b")
    val d3 = D("a", "c")

    assertEquals(d1, d1)
    assertEquals(d1, d2)
    assertNotEqual(d1, d3)

    var hd1 = Holder(D("y", "n"))
    var hd2 = Holder(D("y", "n"))
    var hd3 = Holder(D("1", "2"))

    assertEquals(hd1, hd1)
    assertEquals(hd1, hd2)
    assertNotEqual(hd1, hd3)

    var ho1 = Holder(O("+", "-"))
    var ho2 = Holder(O("+", "-"))
    var ho3 = Holder(O("*", "*"))

    assertEquals(ho1, ho1)
    assertNotEqual(ho1, ho2)
    assertNotEqual(ho1, ho3)

    return "OK"
}