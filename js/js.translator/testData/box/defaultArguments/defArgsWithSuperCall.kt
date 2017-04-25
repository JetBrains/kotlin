// EXPECTED_REACHABLE_NODES: 496
package foo

open class A(val a: Int = 1, val b: Int = 2)

class B : A(b = 3)

fun box(): String {
    val b = B()
    if (b.a != 1) return "b.a != 1, it: ${b.a}"
    if (b.b != 3) return "b.a != 3, it: ${b.b}"
    return "OK"
}