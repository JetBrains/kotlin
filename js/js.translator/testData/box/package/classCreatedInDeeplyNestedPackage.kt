// EXPECTED_REACHABLE_NODES: 491
package foo1.foo2.foo3.foo5.foo6.foo7.foo8

fun box() = A().doBox()

class A() {
    fun doBox() = "OK"
}