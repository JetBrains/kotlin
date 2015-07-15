package foo

fun A.f(s: String) = value + s

class A(val value: String) {
    fun bar(s: String) = (::f)(this, s)
}

fun A.baz(s: String) = (::f)(this, s)

fun box(): String {
    val a = A("aaa")

    assertEquals("aaa.bar()", a.bar(".bar()"))
    assertEquals("aaa.baz()", a.baz(".baz()"))

    return "OK"
}
