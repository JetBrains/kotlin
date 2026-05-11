package test

class B : A() {
    override fun foo(x: Int): Int = x + 1
}

fun bar(): Int = B().foo(1)
