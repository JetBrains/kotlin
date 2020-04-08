// FLOW: IN

open class Base {
    open var prop: Int = 0
}

class Derived : Base() {
    override var prop: Int = 1
}

fun foo(b: Base, d: Derived) {
    b.prop = 10
    val <caret>v = d.prop
}
