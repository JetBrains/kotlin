package test

interface Collector<in T> {
    fun emit(value: T)
}

inline fun <T, C : MutableCollection<in T>> Collector<T>.toCollection(destination: C): C = destination
