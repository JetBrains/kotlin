package foo

data class Holder<T>(val v: T)

data class D(val start: String, val end: String)

class O(val start: String, val end: String)

fun assertEquals<T>(expected: T, actual: T) {
    if (expected != actual) throw Exception("expected: $expected, actual, $actual")
}

fun box(): String {
    val d = D("a", "b")

    assertEquals("#D(start=a, end=b)", "#" + d)

    var hd = Holder(D("y", "n"))

    assertEquals("#Holder(v=D(start=y, end=n))", "#" + hd)

    var ho = Holder(O("+", "-"))

    assertEquals("#Holder(v=[object Object])", "#" + ho)

    return "OK"
}