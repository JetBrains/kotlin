package test

open class Child : Parent(), I {
    final override fun foo(): String = "child-final"
}
