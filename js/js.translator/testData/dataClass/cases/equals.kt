package foo

data class Holder<T>(val v: T)

data class Dat(val start: String, val end: String)

data class Dat2(val start: String, val end: String)

class Obj(val start: String, val end: String)

fun assertEquals<T>(expected: T, actual: T) {
    if (expected != actual) throw Exception("expected: $expected, actual, $actual")
}

fun assertNotEqual<T>(expected: T, actual: T) {
    if (expected == actual) throw Exception("unexpectedly equal: $expected and $actual")
}

fun box(): String {
    val d1 = Dat("a", "b")
    val d2 = Dat("a", "b")
    val d3 = Dat("a", "c")

    val otherD1 = Dat2("a", "b")

    assertEquals(d1, d1)
    assertEquals(d1, d2)
    assertNotEqual(d1, d3)

    assertNotEqual(d1, otherD1)

    var hd1 = Holder(Dat("y", "n"))
    var hd2 = Holder(Dat("y", "n"))
    var hd3 = Holder(Dat("1", "2"))

    assertEquals(hd1, hd1)
    assertEquals(hd1, hd2)
    assertNotEqual(hd1, hd3)

    var ho1 = Holder(Obj("+", "-"))
    var ho2 = Holder(Obj("+", "-"))
    var ho3 = Holder(Obj("*", "*"))

    assertEquals(ho1, ho1)
    assertNotEqual(ho1, ho2)
    assertNotEqual(ho1, ho3)

    return "OK"
}