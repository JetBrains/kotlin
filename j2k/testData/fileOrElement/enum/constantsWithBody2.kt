// ERROR: Cannot access 'p': it is invisible (private in a supertype) in 'A'
enum class E private constructor(private val p: Int) {
    A(1) {
        override fun bar() {
            foo(this.p)
        }
    },

    B(2) {
        override fun bar() {}
    };

    internal fun foo(p: Int) {}

    internal abstract fun bar()
}