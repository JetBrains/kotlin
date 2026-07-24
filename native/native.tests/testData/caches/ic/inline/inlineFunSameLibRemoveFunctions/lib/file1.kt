package test

interface Collector<in T> {
    fun emit(value: T)
}

inline fun <T, K, V> Collector<T>.associate(crossinline transform: (T) -> Pair<K, V>): Map<K, V> {
    val result = LinkedHashMap<K, V>()
    return result
}

inline fun <T, K> Collector<T>.associateBy(crossinline keySelector: (T) -> K): Map<K, T> {
    val result = LinkedHashMap<K, T>()
    return result
}

inline fun <T, K, V> Collector<T>.associateBy(
    crossinline keySelector: (T) -> K, crossinline valueTransform: (T) -> V
): Map<K, V> {
    val result = LinkedHashMap<K, V>()
    return result
}

inline fun <T, K, M : MutableMap<in K, in T>> Collector<T>.associateByTo(
    destination: M, crossinline keySelector: (T) -> K
): M = destination

inline fun <T, K, V, M : MutableMap<in K, in V>> Collector<T>.associateByTo(
    destination: M, crossinline keySelector: (T) -> K, crossinline valueTransform: (T) -> V
): M = destination

inline fun <T, K, V, M : MutableMap<in K, in V>> Collector<T>.associateTo(
    destination: M, crossinline transform: (T) -> Pair<K, V>
): M = destination

inline fun <K, V> Collector<K>.associateWith(crossinline valueSelector: (K) -> V): Map<K, V> {
    val result = LinkedHashMap<K, V>()
    return result
}

inline fun <K, V, M : MutableMap<in K, in V>> Collector<K>.associateWithTo(
    destination: M, crossinline valueSelector: (K) -> V
): M = destination

inline fun <T, K> Collector<T>.groupBy(crossinline keySelector: (T) -> K): Map<K, List<T>> {
    val result = LinkedHashMap<K, MutableList<T>>()
    return result
}

inline fun <T, K, V> Collector<T>.groupBy(
    crossinline keySelector: (T) -> K, crossinline valueTransform: (T) -> V
): Map<K, List<V>> {
    val result = LinkedHashMap<K, MutableList<V>>()
    return result
}

inline fun <T, K, M : MutableMap<in K, MutableList<T>>> Collector<T>.groupByTo(
    destination: M, crossinline keySelector: (T) -> K
): M = destination

inline fun <T, K, V, M : MutableMap<in K, MutableList<V>>> Collector<T>.groupByTo(
    destination: M, crossinline keySelector: (T) -> K, crossinline valueTransform: (T) -> V
): M = destination

inline fun <T, C : MutableCollection<in T>> Collector<T>.toCollection(destination: C): C = destination
