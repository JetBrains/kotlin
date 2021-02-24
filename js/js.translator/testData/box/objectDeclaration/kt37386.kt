// EXPECTED_REACHABLE_NODES: 1315

fun foo() = "OK"

open class A(val foo: Boolean = true) {
    val ok = foo()
}

val q = object : A() {}

fun box() = q.ok