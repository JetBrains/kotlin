package test

open class Child : Parent(), I {
    override fun foo(): String = "child-open"
}
