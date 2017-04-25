// EXPECTED_REACHABLE_NODES: 495
interface I {
    fun foo() = "OK"
}

class A : I

fun box() = A().foo()