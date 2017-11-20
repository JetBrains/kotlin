// EXPECTED_REACHABLE_NODES: 1116
interface I {
    val foo: String
        get() = "OK"
}

class A : I

fun box() = A().foo