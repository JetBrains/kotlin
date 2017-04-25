// EXPECTED_REACHABLE_NODES: 500
package foo

open class A {
    open val a = 2
        get() {
            return field + 1
        }
}

class B : A() {
    override val a: Int = 5
        get() {
            return super.a + 10 * field + 100
        }
}

fun box(): String {
    val a = A()
    val b = B()

    if (a.a != 3) return "a.a != 3, it: ${a.a}"
    if (b.a != 153) return "b.a != 153, it: ${b.a}"

    return "OK"
}