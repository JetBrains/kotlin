package test
open class A { open fun foo(): Int = 1 }
open class B : A() { override fun foo(): Int = 2 }
