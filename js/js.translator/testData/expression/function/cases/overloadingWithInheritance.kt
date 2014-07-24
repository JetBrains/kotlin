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

fun box(): String {
    assertEquals("A", C().foo(1))
    assertEquals("B", C().foo(""))
    assertEquals("C", C().foo())

    return "OK"
}
