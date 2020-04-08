enum class E {
    A,

    B {
        override fun bar() {}
    };

    internal open fun bar() {}
}
