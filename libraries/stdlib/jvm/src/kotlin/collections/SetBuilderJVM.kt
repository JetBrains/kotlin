@file:UseExperimental(kotlin.experimental.ExperimentalTypeInference::class)

package kotlin.collections

import java.util.SortedSet
import java.util.TreeSet

/**
 * Build a new [SortedSet] with the [elements][E] from the [builderAction].
 *
 * @sample samples.collections.Builders.Sets.buildSortedSet
 */
@SinceKotlin("1.3")
@ExperimentalStdlibApi
public inline fun <E> sortedSet(
    @BuilderInference builderAction: SortedSet<E>.() -> Unit
): SortedSet<E> = TreeSet<E>().apply(builderAction)

/**
 * Build a new [SortedSet] with the given [comparator] and [elements][E] from
 * the [builderAction].
 *
 * @sample samples.collections.Builders.Sets.buildSortedSet
 */
@SinceKotlin("1.3")
@ExperimentalStdlibApi
public inline fun <E> sortedSet(
    comparator: Comparator<E>,
    @BuilderInference builderAction: SortedSet<E>.() -> Unit
): SortedSet<E> = TreeSet<E>(comparator).apply(builderAction)

/**
 * Build a new [SortedSet] with the given [comparator] and [elements][E] from
 * the [builderAction].
 *
 * @sample samples.collections.Builders.Sets.buildSortedSet
 */
@SinceKotlin("1.3")
@ExperimentalStdlibApi
public inline fun <E> sortedSet(
    noinline comparator: (lhs: E, rhs: E) -> Int,
    @BuilderInference builderAction: SortedSet<E>.() -> Unit
): SortedSet<E> = sortedSet(Comparator(comparator), builderAction)

/**
 * Build a new [SortedSet] with the [elements][E] and [Comparator] from the
 * given [seed] and then apply the [builderAction].
 *
 * @sample samples.collections.Builders.Sets.buildSortedSet
 */
@SinceKotlin("1.3")
@ExperimentalStdlibApi
public inline fun <E> sortedSet(
    seed: SortedSet<E>,
    @BuilderInference builderAction: SortedSet<E>.() -> Unit
): SortedSet<E> = TreeSet(seed).apply(builderAction)
