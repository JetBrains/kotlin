enum class E {
    O,
    A {
        override fun foo(n: Int, s: String): Int = n + 1

    },
    B {
        override fun foo(n: Int, s: String): Int = n + 2

    };

    open fun foo(n: Int, s: String): Int = n
}