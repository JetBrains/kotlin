class C {
    fun String.foo() {
        with(1) {
            bar()
        }
    }

    fun Int.bar() {}
}