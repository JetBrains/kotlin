interface Producer<out T> {
    fun produce(): T
}

interface Consumer<in T> {
    fun consume(v: T): Int
}
