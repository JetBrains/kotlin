open class A {
    open val pr<caret>op = 5
}

open class B : A() {
    override val prop = 4
}

class B2 : A() {
    final override val prop = 4
}

class C : B()

open class D : A() {
    override val prop = 4
}

open class E : D() {
    override val prop = 4
}

open class E2 : D() {
    final override val prop = 4
}