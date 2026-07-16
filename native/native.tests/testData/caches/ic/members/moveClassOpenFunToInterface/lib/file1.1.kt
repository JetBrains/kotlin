package test

interface I {
    fun foo(): String = "interface"
}

open class Parent

open class Child : Parent(), I
