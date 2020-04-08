enum class <caret>E {
    O,
    A {
        override fun foo(n: Int): Int = n + 1

    },
    B {
        override fun foo(n: Int): Int = n + 2

    };

    open fun foo(n: Int): Int = n
}

// REF: (in E).A
// REF: (in E).B