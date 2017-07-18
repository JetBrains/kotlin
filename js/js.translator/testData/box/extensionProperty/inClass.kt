// EXPECTED_REACHABLE_NODES: 997
class A

class B {
    val A.x: String
        get() = "OK"

    fun result(a: A) = a.x
}

fun box() = B().result(A())
