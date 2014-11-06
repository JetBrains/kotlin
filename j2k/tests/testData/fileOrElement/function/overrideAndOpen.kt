open class A {
    open fun foo() {
    }
}

open class B : A() {
    override fun foo() {
    }
}

class C : B() {
    override fun foo() {
    }
}