// EXPECTED_REACHABLE_NODES: 499
package foo

open class A {
    open fun foo(a: Int = 1) = a
}

class B : A() {
    override fun foo(a: Int) = a + 1
}

fun box(): String {
    val result = B().foo()
    if (result != 2) return "fail: $result"
    return "OK"
}