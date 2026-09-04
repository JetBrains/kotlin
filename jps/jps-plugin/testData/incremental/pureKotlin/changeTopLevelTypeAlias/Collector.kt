package a

fun interface Collector<in T> {
    fun emit(value: T)
}