// EXPECTED_REACHABLE_NODES: 1288
interface I {
    fun foo() = "OK"
}

class A : I

fun box() = A().foo()