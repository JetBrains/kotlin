interface First<T> {
    fun foo(a: T)
}

interface Second<U> : First<Int> {
    val a: U
}