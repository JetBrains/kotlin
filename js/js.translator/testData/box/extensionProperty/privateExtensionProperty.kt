// IGNORE_BACKEND: JS_IR
// EXPECTED_REACHABLE_NODES: 1113
class A {
    fun result() = "OK"
}

private val A.foo: String
    get() = result()

fun box() = A().foo