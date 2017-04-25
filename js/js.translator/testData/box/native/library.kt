// EXPECTED_REACHABLE_NODES: 489
@file:Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER")
package foo

@library class A() {
    @library fun f() {
    }
    @library fun f(a: Int) {
    }
}

@library fun getResult() = false

fun box(): String {
    val a = A()
    a.f()
    a.f(2)
    return if (getResult()) "OK" else "fail"
}
