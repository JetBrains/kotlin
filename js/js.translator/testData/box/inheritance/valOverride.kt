// EXPECTED_REACHABLE_NODES: 1291
package foo

open class C() {
    open val a = 1
}

class D() : C() {
    override val a = 2
}

fun box(): String {
    val d: C = D()
    if (d.a != 2) return "fail: ${d.a}"
    return "OK"
}