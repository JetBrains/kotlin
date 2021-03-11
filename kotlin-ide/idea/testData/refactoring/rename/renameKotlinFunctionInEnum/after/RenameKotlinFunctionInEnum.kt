package test

enum class E {
    O,
    A {
        override fun bar(n: Int): Int = n + 1

    },
    B {
        override fun bar(n: Int): Int = n + 2

    };

    open fun bar(n: Int): Int = n
}