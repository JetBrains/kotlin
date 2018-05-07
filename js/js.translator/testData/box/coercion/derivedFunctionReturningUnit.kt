// EXPECTED_REACHABLE_NODES: 1120
var log = ""

abstract class A<out T> {
    abstract fun foo(): T
}

class B() : A<Unit>() {
    override fun foo() {
        log += "B.foo()"
    }
}

fun box(): String {
    val a: A<Any> = B()
    if (a.foo() != Unit) return "fail1"
    if (log != "B.foo()") return "fail2"

    return "OK"
}