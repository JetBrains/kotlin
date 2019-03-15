// EXPECTED_REACHABLE_NODES: 1288
interface I {
    val foo: String
        get() = "OK"
}

class A : I

fun box() = A().foo