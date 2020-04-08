class Test<T> {
    val <S> <caret>S.bar: Int
        get() = 1

    fun test() {
        "".bar
    }
}