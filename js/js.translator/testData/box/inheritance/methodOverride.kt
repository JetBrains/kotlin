// EXPECTED_REACHABLE_NODES: 499
package foo

open class A {
    fun f1(): Int {
        return 1
    }
    open fun f2(): Int {
        return 3
    }
}

class B : A() {
    override fun f2(): Int {
        return 2
    }
}

fun box(): String {
    val a = A()
    if (a.f1() != 1) return "a.f1() != 1, it: ${a.f1()}"
    if (a.f2() != 3) return "a.f2() != 3, it: ${a.f2()}"

    val b = B();
    if (b.f1() != 1) return "b.f1() != 1, it: ${b.f1()}"
    if (b.f2() != 2) return "b.f2() != 2, it: ${b.f2()}"
    return "OK"
}