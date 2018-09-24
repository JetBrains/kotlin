// EXPECTED_REACHABLE_NODES: 1285
class A {
    fun result() = "OK"
}

private val A.foo: String
    get() = result()

fun box() = A().foo