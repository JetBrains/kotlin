package test

open class Foo {
    open fun foo(): Int = 42
}

open class Child : Foo()

interface I {
    fun foo(): Int
}

open class InterfaceChild : Foo(), I
