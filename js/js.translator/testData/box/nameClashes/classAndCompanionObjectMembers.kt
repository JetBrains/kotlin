// EXPECTED_REACHABLE_NODES: 501
package foo

class A {
    fun foo() = 23

    val bar = 123

    companion object {
        fun foo() = 42

        val bar = 142
    }
}

fun box(): String {
    assertEquals(23, A().foo())
    assertEquals(42, A.foo())

    assertEquals(123, A().bar)
    assertEquals(142, A.bar)

    return "OK"
}