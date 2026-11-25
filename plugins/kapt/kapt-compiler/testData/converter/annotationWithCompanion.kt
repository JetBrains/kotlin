annotation class Foo(val value: String) {
    companion object {
        @JvmStatic
        fun create(value: String) = Foo(value)

        const val CONST = 42

        @JvmStatic
        val named: String = "x"
    }
}
