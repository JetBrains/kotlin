// IGNORE_BACKEND: JS_IR
// EXPECTED_REACHABLE_NODES: 1122
interface I {
    fun foo() = "OK"
}

abstract class A : I

class B : A()

fun box() = B().foo()