class C {
    fun foo(s: String) {
        with(1) {
            bar()
        }
    }

    fun Int.bar() {}
}