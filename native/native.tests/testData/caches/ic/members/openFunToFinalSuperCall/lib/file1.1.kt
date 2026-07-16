package test

open class A {
    fun foo(): Int = 2
}

open class B : A()

open class C : B() {
    open fun bar(): Int = super.foo() + 1
}
