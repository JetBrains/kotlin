package test

open class A {
    open fun foo(): Int = 1
}

open class B : A()

open class C : B() {
    open fun bar(): Int = super.foo() + 1
}
