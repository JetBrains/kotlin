// EXPECTED_REACHABLE_NODES: 1294
interface I {
    fun foo() = "OK"
}

abstract class A : I

class B : A()

fun box() = B().foo()