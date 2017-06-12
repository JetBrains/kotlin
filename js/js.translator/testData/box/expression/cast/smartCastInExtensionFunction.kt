// EXPECTED_REACHABLE_NODES: 505
package foo

class A {
    fun foo(a: Int) = "A.foo($a)"
}

fun Any.bar() = "Any.bar()"
fun A.bar() = "A.bar()"

fun boo(a: Any) = "boo(Any)"
fun boo(a: A) = "boo(A)"

fun Any.testInTopLevel() {
    assertEquals(bar(), "Any.bar()", "bar()")
    assertEquals(this.bar(), "Any.bar()", "this.bar()")
    assertEquals(boo(this), "boo(Any)", "boo(this)")

    if (this is A) {
        assertEquals(foo(47), "A.foo(47)", "foo(47)")
        assertEquals(bar(), "A.bar()", "bar()")

        assertEquals(this.foo(47), "A.foo(47)", "this.foo(47)")
        assertEquals(this.bar(), "A.bar()", "this.bar()")

        assertEquals(boo(this), "boo(A)", "boo(this: A)")
    }
}

class B {
    fun Any.test() {
        assertEquals(bar(), "Any.bar()", "bar()")
        assertEquals(this.bar(), "Any.bar()", "this.bar()")
        assertEquals(boo(this), "boo(Any)", "boo(this)")

        if (this is A) {
            assertEquals(foo(47), "A.foo(47)", "foo(47)")
            assertEquals(bar(), "A.bar()", "bar()")

            assertEquals(this.foo(47), "A.foo(47)", "this.foo(47)")
            assertEquals(this.bar(), "A.bar()", "this.bar()")

            assertEquals(boo(this), "boo(A)", "boo(this: A)")
        }
    }

    fun testInClass() {
        A().test()
    }
}

fun box(): String {
    A().testInTopLevel()
    B().testInClass()
    return "OK"
}
