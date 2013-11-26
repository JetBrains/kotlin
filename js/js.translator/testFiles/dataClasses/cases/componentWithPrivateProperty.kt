package foo

data class A(private val a: Int, protected val b: String) {
    fun c1(): Int = component1()
    fun c2(): String = component2()
}

fun box(): String {
    val t = A(123, "abba")

    val a = t.c1()
    if (a != 123) return "a /*$a*/ != 123"

    val b = t.c2()
    if (b != "abba") return "b /*$b*/ != abba"

    val v = t.copy()

    val c = v.c1()
    if (c != 123) return "c /*$c*/ != 123"

    val d = t.c2()
    if (d != "abba") return "d /*$d*/ != abba"

    val x = t.copy(a = 456)

    val e = x.c1()
    if (e != 456) return "e /*$e*/ != 456"

    val f = x.c2()
    if (f != "abba") return "f /*$f*/ != abba"

    val y = t.copy(b = "cafebabe")

    val g = y.c1()
    if (g != 123) return "g /*$g*/ != 123"

    val h = y.c2()
    if (h != "cafebabe") return "h /*$h*/ != cafebabe"

    val z = t.copy(a = 456, b = "cafebabe")

    val i = z.c1()
    if (i != 456) return "i /*$i*/ != 123"

    val j = z.c2()
    if (j != "cafebabe") return "j /*$j*/ != cafebabe"

    return "OK"
}
