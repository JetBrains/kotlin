package foo

data class Holder<T>(val v: T)

data class Dat(val start: String, val end: String)

class Obj(val start: String, val end: String)

fun box(): String {
    val setD = java.util.HashSet<Holder<Dat>>()
    setD.add(Holder(Dat("a", "b")))
    setD.add(Holder(Dat("a", "b")))
    setD.add(Holder(Dat("a", "b")))
    assertEquals(1, setD.size())

    val setO = java.util.HashSet<Holder<Obj>>()
    setO.add(Holder(Obj("a", "b")))
    setO.add(Holder(Obj("a", "b")))
    setO.add(Holder(Obj("a", "b")))
    assertEquals(3, setO.size())

    return "OK"
}