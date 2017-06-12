// EXPECTED_REACHABLE_NODES: 510
package foo

data class Holder<T>(val v: T)

data class Dat(val start: String, val end: String)

class Obj(val start: String, val end: String)

fun box(): String {
    val d = Dat("a", "b")

    assertEquals("Dat(start=a, end=b)", "${d}")

    var hd = Holder(Dat("y", "n"))

    assertEquals("Holder(v=Dat(start=y, end=n))", "${hd}")

    var ho = Holder(Obj("+", "-"))

    assertEquals("Holder(v=[object Object])", "${ho}")

    return "OK"
}