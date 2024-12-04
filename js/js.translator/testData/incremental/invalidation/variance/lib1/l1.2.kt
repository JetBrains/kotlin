interface Producer<T> {
    fun produce(): T
}

interface Consumer<T> {
    fun consume(v: T): Int
}
