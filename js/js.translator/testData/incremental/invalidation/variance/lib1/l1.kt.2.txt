// TODO: After KT-51099, m.kt from main module must be dirty
interface Producer<T> {
    fun produce(): T
}

interface Consumer<T> {
    fun consume(v: T): Int
}
