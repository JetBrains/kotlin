// IGNORE_BACKEND: JS_IR
// EXPECTED_REACHABLE_NODES: 1292
abstract class A<out T> {
    abstract fun foo(): T
}

class B() : A<Char>() {
    override fun foo() = 'Q'
}

private fun typeOf(x: dynamic) = js("typeof x")

fun box(): String {
    val a: A<Any> = B()
    if (typeOf(a.foo()) != "object") return "fail1"
    if (typeOf(B().foo()) != "number") return "fail2"
    return "OK"
}