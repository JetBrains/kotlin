// EXPECTED_REACHABLE_NODES: 505
package foo

interface A {
    fun foo(i: Int) = "A"
}

interface B {
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
