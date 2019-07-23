// EXPECTED_REACHABLE_NODES: 1305

private class A {
    private val f = "OK"
    inline fun ii() = f
}


private class B {
    private val a = A()
    fun foo() = a.ii()
}

fun box() = B().foo()