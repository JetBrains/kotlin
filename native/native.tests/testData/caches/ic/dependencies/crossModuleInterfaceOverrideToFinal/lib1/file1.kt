package test

interface I {
    fun foo(): String
}

open class Parent {
    open fun foo(): String = "parent"
}
