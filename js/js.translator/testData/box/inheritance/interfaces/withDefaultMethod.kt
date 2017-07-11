// EXPECTED_REACHABLE_NODES: 998
interface I {
    fun foo() = "OK"
}

class A : I

fun box() = A().foo()