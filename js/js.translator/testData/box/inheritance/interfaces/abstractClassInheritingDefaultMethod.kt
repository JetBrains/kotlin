// EXPECTED_REACHABLE_NODES: 501
interface I {
    fun foo() = "OK"
}

abstract class A : I

class B : A()

fun box() = B().foo()