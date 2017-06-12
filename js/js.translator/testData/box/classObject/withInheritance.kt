// EXPECTED_REACHABLE_NODES: 505
package foo

open class A {
    val a = 3
    fun foo(): Int {
        return 5
    }
    companion object: A() {
        val c = a
    }
}

class B {
    companion object: A() {
    }
}

fun box(): String {
    if (A.a != 3) return "A.a != 3, it: ${A.a}"
    if (A.foo() != 5) return "A.foo() != 5, it: ${A.foo()}"

    val a = A
    if (a.c != 3) return "a = A; a.c != 3, it: ${a.c}"

    if (A().a != 3) return "A().a != 3, it: ${A().a}"

    if (B.a != 3) return "B.a != 3, it: ${B.a}"
    val b = B
    if (b.foo() != 5) return "b = B; b.foo() != 5, it: ${b.foo()}"
    return "OK"
}
