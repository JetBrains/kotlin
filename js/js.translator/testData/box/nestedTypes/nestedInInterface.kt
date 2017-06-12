// EXPECTED_REACHABLE_NODES: 510
package foo

interface A {
    fun foo(): String
    fun bar() = "A.bar;"

    class B {
        fun foo() = "A.B.foo;"
        fun bar() = "A.B.bar;"
    }

    class C : A {
        override fun foo() = "A.C.foo;"
    }

    class D : A {
        override fun foo() = "A.D.foo;"
        override fun bar() = "A.D.bar;"
    }
}

fun box(): String {
    assertEquals("A.B.foo;", A.B().foo())
    assertEquals("A.B.bar;", A.B().bar())

    assertEquals("A.C.foo;", A.C().foo())
    assertEquals("A.bar;", A.C().bar())

    assertEquals("A.D.foo;", A.D().foo())
    assertEquals("A.D.bar;", A.D().bar())

    return "OK"
}
