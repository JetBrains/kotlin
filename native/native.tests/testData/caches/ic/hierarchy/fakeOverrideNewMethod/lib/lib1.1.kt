package test

open class Base {
    open fun foo(): Int = 1
    open fun bar(): Int = 2
}

class Derived : Base()
