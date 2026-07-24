package test

open class A {
    open val x: Int = 10
    open fun foo(): Int = 20
}

class B : A()
