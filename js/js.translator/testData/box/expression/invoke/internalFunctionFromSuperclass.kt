// EXPECTED_REACHABLE_NODES: 1386
abstract class A {
    final internal fun foo() = "OK"
}

class B : A() {
    fun bar() = foo()
}

fun box() = B().bar()