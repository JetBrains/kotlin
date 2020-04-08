class Test<T> {
    fun <caret>T.foo() {}

    fun test(t: T) {
        t.foo()
    }
}