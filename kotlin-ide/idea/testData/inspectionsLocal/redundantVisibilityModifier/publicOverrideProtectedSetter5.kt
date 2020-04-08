// PROBLEM: none

abstract class A {
    open var attribute = "a"
        protected set
}

abstract class B : A() {
    override var attribute = "b"
        set
}

class C : B() {
    <caret>public override var attribute = super.attribute
}

fun main() {
    val c = C()
    c.attribute = "test"
}