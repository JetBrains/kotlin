package test

open class Parent {
    open fun foo(): String = "parent-open"
}

open class Child : Parent()
