package foo

open class C() {
    open val a = 1
}

class D() : C() {
    override val a = 2
}

fun box(): Boolean {
    val d: C = D()
    if (d.a != 2) return false
    return true
}