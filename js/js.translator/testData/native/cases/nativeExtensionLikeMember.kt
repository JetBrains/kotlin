package foo

@native
open class A(val value: String) {
}

class B : A("B") {
    fun bar(): String = "B.bar ${value}"
    var prop: String = "B prop"
}

@native fun A.bar(): String = noImpl

@native var A.prop: String
    get() = noImpl
    set(value) = noImpl

fun box(): String {
    var a: A = A("A")
    val b: B = B()

    assertEquals("A.bar A", a.bar())
    assertEquals("B.bar B", b.bar())

    assertEquals("A.bar A", (A::bar)(a))
    assertEquals("B.bar B", (A::bar)(b))

    a.prop = "prop"
    assertEquals("prop", a.prop)
    assertEquals("prop", (A::prop).get(a))

    a = b
    assertEquals("B.bar B", a.bar())
    assertEquals("B.bar B", (A::bar)(a))

    assertEquals("B prop", a.prop)
    assertEquals("B prop", (A::prop).get(a))

    return "OK"
}
