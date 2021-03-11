class C {
    fun <caret>foo(s: String) {
        1.bar()
    }

    fun Int.bar() {}
}