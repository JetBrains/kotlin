package test

abstract class A {
    abstract val x: Int
    abstract fun foo(): Int
}

class B : A() {
    override val x: Int = 1
    override fun foo(): Int = 2
}
