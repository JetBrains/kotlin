// EXPECTED_REACHABLE_NODES: 1002
interface I {
    fun foo() = "OK"
}

interface J : I

class A : J

fun box() = A().foo()