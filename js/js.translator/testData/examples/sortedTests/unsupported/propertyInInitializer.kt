class Outer() {
    val s = "xyzzy"

    open class InnerBase(public val name: String) {
    }

    class InnerDerived() : InnerBase(s) {
    }

    val x = InnerDerived()
}

fun box(): String {
    val o = Outer()
    return if (o.x.name != "xyzzy") "fail" else "OK"
}
