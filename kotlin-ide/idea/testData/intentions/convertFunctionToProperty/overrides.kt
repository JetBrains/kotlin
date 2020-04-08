package test

interface T {
    fun foo(): Boolean
}

open class A(val n: Int): T {
    override fun foo(): Boolean = n > 1
}

class B: A(1) {
    override fun <caret>foo(): Boolean = true
}

fun test() {
    val a = A(1).foo()
    val b = B().foo()
}