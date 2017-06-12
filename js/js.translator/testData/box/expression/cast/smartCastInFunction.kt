// EXPECTED_REACHABLE_NODES: 504
package foo

class A {
    fun foo(a: Int) = "A.foo($a)"
}

fun Any.bar() = "Any.bar()"
fun A.bar() = "A.bar()"

fun boo(a: Any) = "boo(Any)"
fun boo(a: A) = "boo(A)"

fun testInTopLevel(a: Any) {
    assertEquals(a.bar(), "Any.bar()", "bar()")
    assertEquals(a.bar(), "Any.bar()", "this.bar()")
    assertEquals(boo(a), "boo(Any)", "boo(this)")

    if (a is A) {
        assertEquals(a.foo(47), "A.foo(47)", "a.foo(47)")
        assertEquals(a.bar(), "A.bar()", "a.bar()")
        assertEquals(boo(a), "boo(A)", "boo(a: A)")
    }
}

class B {
    fun testInClass(a: Any) {
        assertEquals(a.bar(), "Any.bar()", "bar()")
        assertEquals(a.bar(), "Any.bar()", "this.bar()")
        assertEquals(boo(a), "boo(Any)", "boo(this)")

        if (a is A) {
            assertEquals(a.foo(47), "A.foo(47)", "a.foo(47)")
            assertEquals(a.bar(), "A.bar()", "a.bar()")
            assertEquals(boo(a), "boo(A)", "boo(a: A)")
        }
    }

}

fun box(): String {
    testInTopLevel(A())
    B().testInClass(A())

    return "OK"
}
