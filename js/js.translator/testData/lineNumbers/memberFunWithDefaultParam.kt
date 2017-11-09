open class A {
    open fun foo(x: Int = 23): Int = x

    fun bar() = 42
}

class B : A() {
    override fun foo(x: Int): Int {
        return x * 2
    }
}

// LINES: 1 2 2 2 2 2 2 2 2 2 4 4 4 * 7 7 10 9 9
