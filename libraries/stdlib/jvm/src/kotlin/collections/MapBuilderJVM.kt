@file:UseExperimental(kotlin.experimental.ExperimentalTypeInference::class)

package kotlin.collections

import java.util.SortedMap
import java.util.TreeMap

/**
 * Build a new [SortedMap] with the [key][K]–[value][V] pairs from the
 * [builderAction].
 *
 * @sample samples.collections.Builders.Maps.buildSortedMap
 */
@SinceKotlin("1.3")
@ExperimentalStdlibApi
public inline fun <K, V> sortedMap(
    @BuilderInference builderAction: SortedMap<K, V>.() -> Unit
): SortedMap<K, V> = TreeMap<K, V>().apply(builderAction)

/**
 * Build a new [SortedMap] with the given [comparator] and [key][K]–[value][V]
 * pairs from the [builderAction].
 *
 * @sample samples.collections.Builders.Maps.buildSortedMap
 */
@SinceKotlin("1.3")
@ExperimentalStdlibApi
public inline fun <K, V> sortedMap(
    comparator: Comparator<K>,
    @BuilderInference builderAction: SortedMap<K, V>.() -> Unit
): SortedMap<K, V> = TreeMap<K, V>(comparator).apply(builderAction)

/**
 * Build a new [SortedMap] with the given [comparator] and [key][K]–[value][V]
 * pairs from the [builderAction].
 *
 * @sample samples.collections.Builders.Maps.buildSortedMap
 */
@SinceKotlin("1.3")
@ExperimentalStdlibApi
public inline fun <K, V> sortedMap(
    noinline comparator: (lhs: K, rhs: K) -> Int,
    @BuilderInference builderAction: SortedMap<K, V>.() -> Unit
): SortedMap<K, V> = sortedMap(Comparator(comparator), builderAction)

/**
 * Build a new [SortedMap] with the [key][K]–[value][V] pairs and [Comparator]
 * from the given [seed] and then apply the [builderAction].
 *
 * @sample samples.collections.Builders.Maps.buildSortedMap
 */
@SinceKotlin("1.3")
@ExperimentalStdlibApi
public inline fun <K, V> sortedMap(
    seed: SortedMap<K, V>,
    @BuilderInference builderAction: SortedMap<K, V>.() -> Unit
): SortedMap<K, V> = TreeMap<K, V>(seed).apply(builderAction)
