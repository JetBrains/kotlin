package foo

open class A {
    open fun foo(a: Int = 1) = a
}

class B : A() {
    override fun foo(a: Int) = a + 1
}

fun box() = (B().foo() == 2)