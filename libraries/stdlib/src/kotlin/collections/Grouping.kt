/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:kotlin.jvm.JvmName("GroupingKt")
@file:kotlin.jvm.JvmMultifileClass

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
    /** Returns an [Iterator] over the elements of the source of this grouping. */
    public fun sourceIterator(): Iterator<T>
    /** Extracts the key of an [element]. */
    public fun keyOf(element: T): K
}

/**
 * Groups elements from the [Grouping] source by key and applies [operation] to the elements of each group sequentially,
 * passing the previously accumulated value and the current element as arguments, and stores the results in a new map.
 *
 * The key for each element is provided by the [Grouping.keyOf] function.
 *
 * @param operation function is invoked on each element with the following parameters:
 *  - `key`: the key of the group this element belongs to;
 *  - `accumulator`: the current value of the accumulator of the group, can be `null` if it's the first `element` encountered in the group;
 *  - `element`: the element from the source being aggregated;
 *  - `first`: indicates whether it's the first `element` encountered in the group.
 *
 * @return a [Map] associating the key of each group with the result of aggregation of the group elements.
 * @sample samples.collections.Grouping.aggregateByRadix
 */
@SinceKotlin("1.1")
public inline fun <T, K, R> Grouping<T, K>.aggregate(
    operation: (key: K, accumulator: R?, element: T, first: Boolean) -> R
): Map<K, R> {
    return aggregateTo(mutableMapOf<K, R>(), operation)
}

/**
 * Groups elements from the [Grouping] source by key and applies [operation] to the elements of each group sequentially,
 * passing the previously accumulated value and the current element as arguments,
 * and stores the results in the given [destination] map.
 *
 * The key for each element is provided by the [Grouping.keyOf] function.
 *
 * @param operation a function that is invoked on each element with the following parameters:
 *  - `key`: the key of the group this element belongs to;
 *  - `accumulator`: the current value of the accumulator of the group, can be `null` if it's the first `element` encountered in the group;
 *  - `element`: the element from the source being aggregated;
 *  - `first`: indicates whether it's the first `element` encountered in the group.
 *
 * If the [destination] map already has a value corresponding to some key,
 * then the elements being aggregated for that key are never considered as `first`.
 *
 * @return the [destination] map associating the key of each group with the result of aggregation of the group elements.
 * @sample samples.collections.Grouping.aggregateByRadixTo
 */
@SinceKotlin("1.1")
public inline fun <T, K, R, M : MutableMap<in K, R>> Grouping<T, K>.aggregateTo(
    destination: M,
    operation: (key: K, accumulator: R?, element: T, first: Boolean) -> R
): M {
    for (e in this.sourceIterator()) {
        val key = keyOf(e)
        val accumulator = destination[key]
        destination[key] = operation(key, accumulator, e, accumulator == null && !destination.containsKey(key))
    }
    return destination
}

/**
 * Groups elements from the [Grouping] source by key and applies [operation] to the elements of each group sequentially,
 * passing the previously accumulated value and the current element as arguments, and stores the results in a new map.
 * An initial value of accumulator is provided by [initialValueSelector] function.
 *
 * @param initialValueSelector a function that provides an initial value of accumulator for each group.
 *  It's invoked with parameters:
 *  - `key`: the key of the group;
 *  - `element`: the first element being encountered in that group.
 *
 * @param operation a function that is invoked on each element with the following parameters:
 *  - `key`: the key of the group this element belongs to;
 *  - `accumulator`: the current value of the accumulator of the group;
 *  - `element`: the element from the source being accumulated.
 *
 * @return a [Map] associating the key of each group with the result of accumulating the group elements.
 * @sample samples.collections.Grouping.foldByEvenLengthWithComputedInitialValue
 */
@SinceKotlin("1.1")
public inline fun <T, K, R> Grouping<T, K>.fold(
    initialValueSelector: (key: K, element: T) -> R,
    operation: (key: K, accumulator: R, element: T) -> R
): Map<K, R> =
    @Suppress("UNCHECKED_CAST")
    aggregate { key, acc, e, first -> operation(key, if (first) initialValueSelector(key, e) else acc as R, e) }

/**
 * Groups elements from the [Grouping] source by key and applies [operation] to the elements of each group sequentially,
 * passing the previously accumulated value and the current element as arguments,
 * and stores the results in the given [destination] map.
 * An initial value of accumulator is provided by [initialValueSelector] function.
 *
 * @param initialValueSelector a function that provides an initial value of accumulator for each group.
 *  It's invoked with parameters:
 *  - `key`: the key of the group;
 *  - `element`: the first element being encountered in that group.
 *
 * If the [destination] map already has a value corresponding to some key, that value is used as an initial value of
 * the accumulator for that group and the [initialValueSelector] function is not called for that group.
 *
 * @param operation a function that is invoked on each element with the following parameters:
 *  - `key`: the key of the group this element belongs to;
 *  - `accumulator`: the current value of the accumulator of the group;
 *  - `element`: the element from the source being accumulated.
 *
 * @return the [destination] map associating the key of each group with the result of accumulating the group elements.
 * @sample samples.collections.Grouping.foldByEvenLengthWithComputedInitialValueTo
 */
@SinceKotlin("1.1")
public inline fun <T, K, R, M : MutableMap<in K, R>> Grouping<T, K>.foldTo(
    destination: M,
    initialValueSelector: (key: K, element: T) -> R,
    operation: (key: K, accumulator: R, element: T) -> R
): M =
    @Suppress("UNCHECKED_CAST")
    aggregateTo(destination) { key, acc, e, first -> operation(key, if (first) initialValueSelector(key, e) else acc as R, e) }


/**
 * Groups elements from the [Grouping] source by key and applies [operation] to the elements of each group sequentially,
 * passing the previously accumulated value and the current element as arguments, and stores the results in a new map.
 * An initial value of accumulator is the same [initialValue] for each group.
 *
 * @param operation a function that is invoked on each element with the following parameters:
 *  - `accumulator`: the current value of the accumulator of the group;
 *  - `element`: the element from the source being accumulated.
 *
 * @return a [Map] associating the key of each group with the result of accumulating the group elements.
 * @sample samples.collections.Grouping.foldByEvenLengthWithConstantInitialValue
 */
@SinceKotlin("1.1")
public inline fun <T, K, R> Grouping<T, K>.fold(
    initialValue: R,
    operation: (accumulator: R, element: T) -> R
): Map<K, R> =
    @Suppress("UNCHECKED_CAST")
    aggregate { _, acc, e, first -> operation(if (first) initialValue else acc as R, e) }

/**
 * Groups elements from the [Grouping] source by key and applies [operation] to the elements of each group sequentially,
 * passing the previously accumulated value and the current element as arguments,
 * and stores the results in the given [destination] map.
 * An initial value of accumulator is the same [initialValue] for each group.
 *
 * If the [destination] map already has a value corresponding to the key of some group,
 * that value is used as an initial value of the accumulator for that group.
 *
 * @param operation a function that is invoked on each element with the following parameters:
 *  - `accumulator`: the current value of the accumulator of the group;
 *  - `element`: the element from the source being accumulated.
 *
 * @return the [destination] map associating the key of each group with the result of accumulating the group elements.
 * @sample samples.collections.Grouping.foldByEvenLengthWithConstantInitialValueTo
 */
@SinceKotlin("1.1")
public inline fun <T, K, R, M : MutableMap<in K, R>> Grouping<T, K>.foldTo(
    destination: M,
    initialValue: R,
    operation: (accumulator: R, element: T) -> R
): M =
    @Suppress("UNCHECKED_CAST")
    aggregateTo(destination) { _, acc, e, first -> operation(if (first) initialValue else acc as R, e) }


/**
 * Groups elements from the [Grouping] source by key and applies the reducing [operation] to the elements of each group
 * sequentially starting from the second element of the group,
 * passing the previously accumulated value and the current element as arguments,
 * and stores the results in a new map.
 * An initial value of accumulator is the first element of the group.
 *
 * @param operation a function that is invoked on each subsequent element of the group with the following parameters:
 *  - `key`: the key of the group this element belongs to;
 *  - `accumulator`: the current value of the accumulator of the group;
 *  - `element`: the element from the source being accumulated.
 *
 * @return a [Map] associating the key of each group with the result of accumulating the group elements.
 * @sample samples.collections.Grouping.reduceByMaxVowels
 */
@SinceKotlin("1.1")
public inline fun <S, T : S, K> Grouping<T, K>.reduce(
    operation: (key: K, accumulator: S, element: T) -> S
): Map<K, S> =
    aggregate { key, acc, e, first ->
        @Suppress("UNCHECKED_CAST")
        if (first) e else operation(key, acc as S, e)
    }

/**
 * Groups elements from the [Grouping] source by key and applies the reducing [operation] to the elements of each group
 * sequentially starting from the second element of the group,
 * passing the previously accumulated value and the current element as arguments,
 * and stores the results in the given [destination] map.
 * An initial value of accumulator is the first element of the group.
 *
 * If the [destination] map already has a value corresponding to the key of some group,
 * that value is used as an initial value of the accumulator for that group and the first element of that group is also
 * subjected to the [operation].

 * @param operation a function that is invoked on each subsequent element of the group with the following parameters:
 *  - `accumulator`: the current value of the accumulator of the group;
 *  - `element`: the element from the source being folded;
 *
 * @return the [destination] map associating the key of each group with the result of accumulating the group elements.
 * @sample samples.collections.Grouping.reduceByMaxVowelsTo
 */
@SinceKotlin("1.1")
public inline fun <S, T : S, K, M : MutableMap<in K, S>> Grouping<T, K>.reduceTo(
    destination: M,
    operation: (key: K, accumulator: S, element: T) -> S
): M =
    aggregateTo(destination) { key, acc, e, first ->
        @Suppress("UNCHECKED_CAST")
        if (first) e else operation(key, acc as S, e)
    }


/**
 * Groups elements from the [Grouping] source by key and counts elements in each group to the given [destination] map.
 *
 * If the [destination] map already has a value corresponding to the key of some group,
 * that value is used as an initial value of the counter for that group.
 *
 * @return the [destination] map associating the key of each group with the count of elements in the group.
 *
 * @sample samples.collections.Grouping.groupingByEachCount
 */
@SinceKotlin("1.1")
public fun <T, K, M : MutableMap<in K, Int>> Grouping<T, K>.eachCountTo(destination: M): M =
    foldTo(destination, 0) { acc, _ -> acc + 1 }

/*
/**
 * Groups elements from the [Grouping] source by key and sums values provided by the [valueSelector] function for elements in each group
 * to the given [destination] map.
 *
 *
 * If the [destination] map already has a value corresponding to the key of some group,
 * that value is used as an initial value of the sum for that group.
 *
 * @return the [destination] map associating the key of each group with the sum of elements in the group.
 */
@SinceKotlin("1.1")
public inline fun <T, K, M : MutableMap<in K, Int>> Grouping<T, K>.eachSumOfTo(destination: M, valueSelector: (T) -> Int): M =
        foldTo(destination, 0) { acc, e -> acc + valueSelector(e)}
*/


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
