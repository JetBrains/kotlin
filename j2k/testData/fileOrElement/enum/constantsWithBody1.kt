public enum class E {
    A,

    B {
        override fun bar() {
        }
    };

    open fun bar() {
    }
}
