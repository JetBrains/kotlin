// EXPECTED_REACHABLE_NODES: 501
package foo

object A {
    val a = 1
    fun foo() = 31

    val f = { a + foo() }
}

class B {
    companion object {
        val a = 21
        fun foo() = 3

        val f = { this.a + this.foo() }
    }
}

fun box(): String {
    val a = A.f()
    if (a != 32) return "a != 32, a = $a"

    val b = B.f()
    if (b != 24) return "b != 24, b = $b"

    return "OK"
}
