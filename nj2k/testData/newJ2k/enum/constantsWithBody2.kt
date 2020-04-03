// ERROR: Cannot access 'p': it is invisible (private in a supertype) in 'A'
enum class E(private val p: Int) {
    A(1) {
        override fun bar() {
            foo(p)
        }
    },
    B(2) {
        override fun bar() {}
    };

    fun foo(p: Int) {}
    abstract fun bar()
}