open class A {
    open fun <caret>foo(n: Int): String = ""
}

class B: A() {
    override fun foo(n: Int): String = ""
}

fun test() {
    A().foo(1)
    B().foo(2)
    X().foo(3)
    Y().foo(4)
}