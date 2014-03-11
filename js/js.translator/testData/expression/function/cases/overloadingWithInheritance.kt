package foo

trait A {
    fun foo(i: Int) = "A"
}

trait B {
    fun foo(s: String) = "B"
}

class C : A, B {
    fun foo() = "C"
}

fun assertEquals(expected: Any, actual: Any) {
    if (expected != actual) throw Exception("expected = $expected\nactual = $actual")
}
fun box(): String {
    assertEquals(C().foo(1), "A")
    assertEquals(C().foo(""), "B")
    assertEquals(C().foo(), "C")

    return "OK"
}
