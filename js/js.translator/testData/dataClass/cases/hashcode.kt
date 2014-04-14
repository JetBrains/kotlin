package foo

data class Holder<T>(val v: T)

data class D(val start: String, val end: String)

class O(val start: String, val end: String)

fun assertEquals<T>(expected: T, actual: T) {
    if (expected != actual) throw Exception("expected: $expected, actual, $actual")
}

fun box(): String {
    val setD = java.util.HashSet<Holder<D>>()
    setD.add(Holder(D("a", "b")))
    setD.add(Holder(D("a", "b")))
    setD.add(Holder(D("a", "b")))
    assertEquals(1, setD.size())

    val setO = java.util.HashSet<Holder<O>>()
    setO.add(Holder(O("a", "b")))
    setO.add(Holder(O("a", "b")))
    setO.add(Holder(O("a", "b")))
    assertEquals(3, setO.size())

    return "OK"
}