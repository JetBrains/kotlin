// IGNORE_BACKEND: JS_IR
// EXPECTED_REACHABLE_NODES: 1120
interface I {
    fun foo() = "OK"
}

interface J : I

class A : J

fun box() = A().foo()