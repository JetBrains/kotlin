// PSI_ELEMENT: org.jetbrains.kotlin.psi.KtNamedFunction
// OPTIONS: usages
enum class E {
    O
    A {
        init {
            foo(1)
        }

        override fun foo(n: Int): Int = n + 1
    }
    B {
        init {
            foo(1)
        }

        override fun foo(n: Int): Int = n + 2
    }

    init {
        foo(1)
    }

    internal open fun <caret>foo(n: Int): Int = n
}
