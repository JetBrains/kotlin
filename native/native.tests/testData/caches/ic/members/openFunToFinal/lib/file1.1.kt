package test

open class Foo {
    fun foo(): Int = 43
}

open class Child : Foo()

interface I {
    fun foo(): Int
}

open class InterfaceChild : Foo(), I
