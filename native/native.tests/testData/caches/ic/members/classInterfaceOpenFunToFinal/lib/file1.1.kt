package test

interface I {
    fun foo(): String
}

open class Parent {
    fun foo(): String = "parent-final"
}

open class Child : Parent(), I
