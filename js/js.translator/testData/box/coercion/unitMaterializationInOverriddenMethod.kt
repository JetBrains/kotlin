// EXPECTED_REACHABLE_NODES: 1279
// KT-51878

open class A {
    open fun foo(): Any = "string"
}

var condition = true

class B: A() {
    override fun foo(): Unit {
        if (condition) {
            return
        }
        condition = !condition
    }
}

fun box(): String {
    val b: A = B()
    if (b.foo() !is Unit) return "fail1"
    if (b.foo() !is Unit) return "fail2"
    return "OK"
}
