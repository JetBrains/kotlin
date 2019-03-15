// EXPECTED_REACHABLE_NODES: 1235
interface I {
    fun foo() = "OK"
}

interface J : I

open class A : J
class B : A()

fun box() = B().foo()