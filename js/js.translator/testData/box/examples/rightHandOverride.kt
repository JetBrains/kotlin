// EXPECTED_REACHABLE_NODES: 500
// Changed when traits were introduced. May not make sense any more

interface Left {
}
open class Right() {
    open fun f() = 42
}

class D() : Left, Right() {
    override fun f() = 239
}

fun box(): String {
    val r: Right = Right()
    val d: D = D()

    if (r.f() != 42) return "Fail #1"
    if (d.f() != 239) return "Fail #2"

    return "OK"
}