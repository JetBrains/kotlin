package test

enum class Op {
    ADD { override fun apply(x: Int): Int = x + 10 },
    MUL { override fun apply(x: Int): Int = x * 2 };

    abstract fun apply(x: Int): Int
}
