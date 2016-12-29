package kotlin.collections

/**
 * Represents a source of elements with a [keyOf] function, which can be applied to each element to get its key.
 *
 * A [Grouping] structure serves as an intermediate step in group-and-fold operations:
 * they group elements by their keys and then fold each group with some aggregating operation.
 *
 * It is created by attaching `keySelector: (T) -> K` function to a source of elements.
 * To get an instance of [Grouping] use one of `groupingBy` extension functions:
 * - [Iterable.groupingBy]
 * - [Sequence.groupingBy]
 * - [Array.groupingBy]
 * - [CharSequence.groupingBy]
 *
 * For the list of group-and-fold operations available, see the [extension functions](#extension-functions) for `Grouping`.
 */
@SinceKotlin("1.1")
public interface Grouping<T, out K> {
    /** Returns an [Iterator] which iterates through the elements of the source. */
    fun elementIterator(): Iterator<T>
    /** Extracts the key of an [element]. */
    fun keyOf(element: T): K
}

/**
 * Groups elements from the [Grouping] source by key and aggregates elements of each group with the specified [operation].
 *
 * The key for each element is provided by the [Grouping.keyOf] function.
 *
 * @param operation function is invoked on each element with the following parameters:
 *  - `key`: the key of a group this element belongs to;
 *  - `value`: the current value of the accumulator of a group, can be `null` if it's first `element` encountered in the group;
 *  - `element`: the element from the source being aggregated;
 *  - `first`: indicates whether it's first `element` encountered in the group.
 *
 * @return a [Map] associating the key of each group with the result of aggregation of the group elements.
 */
@SinceKotlin("1.1")
public inline fun <T, K, R> Grouping<T, K>.aggregate(
        operation: (key: K, value: R?, element: T, first: Boolean) -> R
): Map<K, R> {
    return aggregateTo(mutableMapOf<K, R>(), operation)
}

/**
 * Groups elements from the [Grouping] source by key and aggregates elements of each group with the specified [operation]
 * to the given [destination] map.
 *
 * The key for each element is provided by the [Grouping.keyOf] function.
 *
 * @param operation a function that is invoked on each element with the following parameters:
 *  - `key`: the key of a group this element belongs to;
 *  - `accumulator`: the current value of the accumulator of the group, can be `null` if it's first `element` encountered in the group;
 *  - `element`: the element from the source being aggregated;
 *  - `first`: indicates whether it's first `element` encountered in the group.
 *
 * If the [destination] map already has a value corresponding to some key,
 * then the elements being aggregated for that key are never considered as `first`.
 *
 * @return the [destination] map associating the key of each group with the result of aggregation of the group elements.
 */
@SinceKotlin("1.1")
public inline fun <T, K, R, M : MutableMap<in K, R>> Grouping<T, K>.aggregateTo(
        destination: M,
        operation: (key: K, accumulator: R?, element: T, first: Boolean) -> R
): M {
    for (e in this.elementIterator()) {
        val key = keyOf(e)
        val accumulator = destination[key]
        destination[key] = operation(key, accumulator, e, accumulator == null && !destination.containsKey(key))
    }
    return destination
}

/**
 * Groups elements from the [Grouping] source by key and accumulates elements of each group with the specified [operation]
 * starting with an initial value of accumulator provided by the [initialValueSelector] function.
 *
 * @param initialValueSelector a function that provides an initial value of accumulator for an each group.
 *  It's invoked with parameters:
 *  - `key`: the key of a group;
 *  - `element`: the first element being encountered in that group.
 *
 * @param operation a function that is invoked on each element with the following parameters:
 *  - `key`: the key of a group this element belongs to;
 *  - `accumulator`: the current value of the accumulator of the group;
 *  - `element`: the element from the source being accumulated.
 *
 * @return a [Map] associating the key of each group with the result of accumulating the group elements.
 */
@SinceKotlin("1.1")
public inline fun <T, K, R> Grouping<T, K>.fold(
        initialValueSelector: (key: K, element: T) -> R,
        operation: (key: K, accumulator: R, element: T) -> R
): Map<K, R> =
        aggregate { key, value, e, first -> operation(key, if (first) initialValueSelector(key, e) else value as R, e) }

/**
 * Groups elements from the [Grouping] source by key and accumulates elements of each group with the specified [operation]
 * starting with an initial value of accumulator provided by the [initialValueSelector] function
 * to the given [destination] map.
 *
 * @param initialValueSelector a function that provides an initial value of accumulator for an each group.
 *  It's invoked with parameters:
 *  - `key`: the key of a group;
 *  - `element`: the first element being encountered in that group.
 *
 * If the [destination] map already has a value corresponding to some key, that value is used as an initial value of
 * the accumulator for that group and the [initialValueSelector] function is not called for that group.
 *
 * @param operation a function that is invoked on each element with the following parameters:
 *  - `key`: the key of a group this element belongs to;
 *  - `accumulator`: the current value of the accumulator of the group;
 *  - `element`: the element from the source being accumulated.
 *
 * @return the [destination] map associating the key of each group with the result of accumulating the group elements.
 */
@SinceKotlin("1.1")
public inline fun <T, K, R, M : MutableMap<in K, R>> Grouping<T, K>.foldTo(
        destination: M,
        initialValueSelector: (key: K, element: T) -> R,
        operation: (key: K, accumulator: R, element: T) -> R
): M =
        aggregateTo(destination) { key, value, e, first -> operation(key, if (first) initialValueSelector(key, e) else value as R, e) }


/**
 * Groups elements from the [Grouping] source by key and accumulates elements of each group with the specified [operation]
 * starting with the [initialValue].
 *
 * @param operation a function that is invoked on each element with the following parameters:
 *  - `accumulator`: the current value of the accumulator of the group;
 *  - `element`: the element from the source being accumulated.
 *
 * @return a [Map] associating the key of each group with the result of accumulating the group elements.
 */
@SinceKotlin("1.1")
public inline fun <T, K, R> Grouping<T, K>.fold(
        initialValue: R,
        operation: (accumulator: R, element: T) -> R
): Map<K, R> =
        aggregate { k, v, e, first -> operation(if (first) initialValue else v as R, e) }

/**
 * Groups elements from the [Grouping] source by key and accumulates elements of each group with the specified [operation]
 * starting with the [initialValue] to the given [destination] map.
 *
 * If the [destination] map already has a value corresponding to the key of some group,
 * that value is used as an initial value of the accumulator for that group.
 *
 * @param operation a function that is invoked on each element with the following parameters:
 *  - `accumulator`: the current value of the accumulator of the group;
 *  - `element`: the element from the source being accumulated.
 *
 * @return the [destination] map associating the key of each group with the result of accumulating the group elements.
 */
@SinceKotlin("1.1")
public inline fun <T, K, R, M : MutableMap<in K, R>> Grouping<T, K>.foldTo(
        destination: M,
        initialValue: R,
        operation: (accumulator: R, element: T) -> R
): M =
        aggregateTo(destination) { k, v, e, first -> operation(if (first) initialValue else v as R, e) }


/**
 * Groups elements from the [Grouping] source by key and accumulates elements of each group with the specified [operation]
 * starting the first element in that group.
 *
 * @param operation a function that is invoked on each subsequent element of the group with the following parameters:
 *  - `key`: the key of a group this element belongs to;
 *  - `accumulator`: the current value of the accumulator of the group;
 *  - `element`: the element from the source being accumulated.
 *
 * @return a [Map] associating the key of each group with the result of accumulating the group elements.
 */
@SinceKotlin("1.1")
public inline fun <S, T : S, K> Grouping<T, K>.reduce(
        operation: (key: K, accumulator: S, element: T) -> S
): Map<K, S> =
        aggregate { key, value, e, first ->
            if (first) e else operation(key, value as S, e)
        }

/**
 * Groups elements from the [Grouping] source by key and accumulates elements of each group with the specified [operation]
 * starting the first element in that group to the given [destination] map.
 *
 * If the [destination] map already has a value corresponding to the key of some group,
 * that value is used as an initial value of the accumulator for that group and the first element of that group is also
 * subjected to the [operation].

 * @param operation a function that is invoked on each subsequent element of the group with the following parameters:
 *  - `accumulator`: the current value of the accumulator of the group;
 *  - `element`: the element from the source being folded;
 *
 * @return the [destination] map associating the key of each group with the result of accumulating the group elements.
 */
@SinceKotlin("1.1")
public inline fun <S, T : S, K, M : MutableMap<in K, S>> Grouping<T, K>.reduceTo(
        destination: M,
        operation: (key: K, accumulator: S, element: T) -> S
): M =
        aggregateTo(destination) { key, value, e, first ->
            if (first) e else operation(key, value as S, e)
        }


/**
 * Groups elements from the [Grouping] source by key and counts elements in each group.
 *
 * @return a [Map] associating the key of each group with the count of element in the group.
 */
@SinceKotlin("1.1")
@JvmVersion
public fun <T, K> Grouping<T, K>.eachCount(): Map<K, Int> =
        // fold(0) { acc, e -> acc + 1 } optimized for boxing
        foldTo( destination = mutableMapOf(),
                initialValueSelector = { k, e -> kotlin.jvm.internal.Ref.IntRef() },
                operation = { k, acc, e -> acc.apply { element += 1 } })
        .mapValuesInPlace { it.value.element }

/**
 * Groups elements from the [Grouping] source by key and counts elements in each group to the given [destination] map.
 *
 * @return the [destination] map associating the key of each group with the count of element in the group.
 */
@SinceKotlin("1.1")
public fun <T, K, M : MutableMap<in K, Int>> Grouping<T, K>.eachCountTo(destination: M): M =
        foldTo(destination, 0) { acc, e -> acc + 1 }

/**
 * Groups elements from the [Grouping] source by key and sums values provided by the [valueSelector] function for elements in each group.
 *
 * @return a [Map] associating the key of each group with the count of element in the group.
 */
@SinceKotlin("1.1")
@JvmVersion
public inline fun <T, K> Grouping<T, K>.eachSumOf(valueSelector: (T) -> Int): Map<K, Int> =
        // fold(0) { acc, e -> acc + valueSelector(e)} optimized for boxing
        foldTo( destination = mutableMapOf(),
                initialValueSelector = { k, e -> kotlin.jvm.internal.Ref.IntRef() },
                operation = { k, acc, e -> acc.apply { element += valueSelector(e) } })
        .mapValuesInPlace { it.value.element }

/**
 * Groups elements from the [Grouping] source by key and sums values provided by the [valueSelector] function for elements in each group
 * to the given [destination] map.
 *
 * @return the [destination] map associating the key of each group with the count of element in the group.
 */
@SinceKotlin("1.1")
public inline fun <T, K, M : MutableMap<in K, Int>> Grouping<T, K>.eachSumOfTo(destination: M, valueSelector: (T) -> Int): M =
        foldTo(destination, 0) { acc, e -> acc + valueSelector(e)}


@JvmVersion
@PublishedApi
@kotlin.internal.InlineOnly
internal inline fun <K, V, R> MutableMap<K, V>.mapValuesInPlace(f: (Map.Entry<K, V>) -> R): MutableMap<K, R> {
    entries.forEach {
        (it as MutableMap.MutableEntry<K, R>).setValue(f(it))
    }
    return (this as MutableMap<K, R>)
}

/*
// TODO: sum by long and by double overloads

public inline fun <T, K, M : MutableMap<in K, Long>> Grouping<T, K>.sumEachByLongTo(destination: M, valueSelector: (T) -> Long): M =
        foldTo(destination, 0L) { acc, e -> acc + valueSelector(e)}

public inline fun <T, K> Grouping<T, K>.sumEachByLong(valueSelector: (T) -> Long): Map<K, Long> =
        fold(0L) { acc, e -> acc + valueSelector(e)}

public inline fun <T, K, M : MutableMap<in K, Double>> Grouping<T, K>.sumEachByDoubleTo(destination: M, valueSelector: (T) -> Double): M =
        foldTo(destination, 0.0) { acc, e -> acc + valueSelector(e)}

public inline fun <T, K> Grouping<T, K>.sumEachByDouble(valueSelector: (T) -> Double): Map<K, Double> =
        fold(0.0) { acc, e -> acc + valueSelector(e)}
*/
