// EXPECTED_REACHABLE_NODES: 493
package foo

open class A(val x: Int, val y: Int) {
    inner class B(val z: Int) {
        fun foo() = x + y + z
    }
}

fun box(): String {
    val a = A(2, 3)
    val b = a.B(4)
    return if (b.foo() == 9) "OK" else "failure"
}

