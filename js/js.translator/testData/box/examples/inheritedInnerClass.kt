// EXPECTED_REACHABLE_NODES: 1291
class Outer() {
    open class InnerBase() {
    }

    class InnerDerived() : InnerBase() {
    }

    public val foo: InnerBase? = InnerDerived()
}

fun box(): String {
    val o = Outer()
    return if (o.foo === null) "fail" else "OK"
}
