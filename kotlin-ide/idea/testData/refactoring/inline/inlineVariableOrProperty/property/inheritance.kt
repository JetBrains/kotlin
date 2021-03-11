abstract class A {
    open val <caret>b: Int = 3
}

class B : A() {
    override val b: Int = 42
}

open class C : A() {
    override val b: Int get() = 24
}

class D : C() {
    override val b: Int get() = 33
}