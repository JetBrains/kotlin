// EXPECTED_REACHABLE_NODES: 500
package foo

class A {
    fun test() = 23
}

fun <T : A> T.foo(): Int {
    class B {
        fun foo() = test()
    }
    return B().foo()
}

fun box(): String {
    assertEquals(23, A().foo())
    return "OK"
}