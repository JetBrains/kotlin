// EXPECTED_REACHABLE_NODES: 499
open class Foo() {
    fun xyzzy(): String = "xyzzy"
}

class Bar() : Foo() {
    fun test(): String = xyzzy()
}

fun box(): String {
    val bar = Bar()
    val f = bar.test()
    return if (f == "xyzzy") "OK" else "fail"
}
