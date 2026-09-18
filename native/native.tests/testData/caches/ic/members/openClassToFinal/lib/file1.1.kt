package test

open class Parent {
    open fun foo(): String = "parent-open"
}

class Child : Parent()
