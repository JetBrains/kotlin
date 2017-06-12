// EXPECTED_REACHABLE_NODES: 507
open class M() {
    open var b: Int = 0
}

class N() : M() {
    val a: Int
        get() {
            super.b = super.b + 1
            return super.b + 1
        }
    override var b: Int = a + 1

    val superb: Int
        get() = super.b
}

fun box(): String {
    val n = N()
    println("a: " + n.a + " b: " + n.b + " superb: " + n.superb)
    if (n.b == 3 && n.a == 4 && n.superb == 3) return "OK";
    return "fail";
}