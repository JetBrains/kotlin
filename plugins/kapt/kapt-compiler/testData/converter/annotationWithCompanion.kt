annotation class Foo(val value: String) {
    companion object {
        @JvmStatic
        fun create(value: String) = Foo(value)
    }
}
