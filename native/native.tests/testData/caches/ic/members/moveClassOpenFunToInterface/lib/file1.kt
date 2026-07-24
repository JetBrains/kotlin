package test

interface I

open class Parent {
    open fun foo(): String = "class-open"
}

open class Child : Parent(), I
