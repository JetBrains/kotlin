// EXPECTED_REACHABLE_NODES: 1382
interface I {
    fun foo() = "OK"
}

class A : I

fun box() = A().foo()