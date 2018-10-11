/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

@file:kotlin.jvm.JvmName("GroupingKt")
@file:kotlin.jvm.JvmMultifileClass

package kotlin.collections


/**
 * Groups elements from the [Grouping] source by key and counts elements in each group.
 *
 * @return a [Map] associating the key of each group with the count of elements in the group.
 *
 * @sample samples.collections.Grouping.groupingByEachCount
 */
@SinceKotlin("1.1")
public actual fun <T, K> Grouping<T, K>.eachCount(): Map<K, Int> =
// fold(0) { acc, e -> acc + 1 } optimized for boxing
    foldTo(destination = mutableMapOf(),
           initialValueSelector = { _, _ -> kotlin.jvm.internal.Ref.IntRef() },
           operation = { _, acc, _ -> acc.apply { element += 1 } })
        .mapValuesInPlace { it.value.element }

/*
/**
 * Groups elements from the [Grouping] source by key and sums values provided by the [valueSelector] function for elements in each group.
 *
 * @return a [Map] associating the key of each group with the sum of elements in the group.
 */
@SinceKotlin("1.X")
public inline fun <T, K> Grouping<T, K>.eachSumOf(valueSelector: (T) -> Int): Map<K, Int> =
        // fold(0) { acc, e -> acc + valueSelector(e)} optimized for boxing
        foldTo( destination = mutableMapOf(),
                initialValueSelector = { _, _ -> kotlin.jvm.internal.Ref.IntRef() },
                operation = { _, acc, e -> acc.apply { element += valueSelector(e) } })
        .mapValuesInPlace { it.value.element }
*/



@PublishedApi
@kotlin.internal.InlineOnly
@Suppress("UNCHECKED_CAST") // tricks with erased generics go here, do not repeat on reified platforms
internal inline fun <K, V, R> MutableMap<K, V>.mapValuesInPlace(f: (Map.Entry<K, V>) -> R): MutableMap<K, R> {
    entries.forEach {
        (it as MutableMap.MutableEntry<K, R>).setValue(f(it))
    }
    return (this as MutableMap<K, R>)
}