/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:kotlin.jvm.JvmName("TuplesKt")

package kotlin

import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import kotlin.internal.InlineOnly


/**
 * Represents a generic pair of two values.
 *
 * There is no meaning attached to values in this class, it can be used for any purpose.
 * Pair exhibits value semantics, i.e. two pairs are equal if both components are equal.
 *
 * An example of decomposing it into values:
 * @sample samples.misc.Tuples.pairDestructuring
 *
 * @param A type of the first value.
 * @param B type of the second value.
 * @property first First value.
 * @property second Second value.
 * @constructor Creates a new instance of Pair.
 */
public data class Pair<out A, out B>(
    public val first: A,
    public val second: B
) : Serializable {

    /**
     * Returns string representation of the [Pair] including its [first] and [second] values.
     */
    public override fun toString(): String = "($first, $second)"
}

/**
 * Creates a tuple of type [Pair] from this and [that].
 *
 * This can be useful for creating [Map] literals with less noise, for example:
 * @sample samples.collections.Maps.Instantiation.mapFromPairs
 */
public infix fun <A, B> A.to(that: B): Pair<A, B> = Pair(this, that)

/**
 * Converts this pair into a list.
 * @sample samples.misc.Tuples.pairToList
 */
public fun <T> Pair<T, T>.toList(): List<T> = listOf(first, second)

/**
 * Transforms the first component of a [Pair] by applying the given [transform]
 * function.
 *  @sample samples.misc.Tuples.pairMapFirst
 */
public inline fun <A, B, T> Pair<A, B>.mapFirst(transform: (A) -> T): Pair<T, B> {
    contract {
        callsInPlace(transform, InvocationKind.EXACTLY_ONCE)
    }
    return Pair(transform(first), second)
}

/**
 * Transforms the second component of a [Pair] by applying the given [transform]
 * function.
 *  @sample samples.misc.Tuples.pairMapSecond
 */
public inline fun <A, B, T> Pair<A, B>.mapSecond(transform: (B) -> T): Pair<A, T> {
    contract {
        callsInPlace(transform, InvocationKind.EXACTLY_ONCE)
    }
    return Pair(first, transform(second))
}

/**
 * Represents a triad of values
 *
 * There is no meaning attached to values in this class, it can be used for any purpose.
 * Triple exhibits value semantics, i.e. two triples are equal if all three components are equal.
 * An example of decomposing it into values:
 * @sample samples.misc.Tuples.tripleDestructuring
 *
 * @param A type of the first value.
 * @param B type of the second value.
 * @param C type of the third value.
 * @property first First value.
 * @property second Second value.
 * @property third Third value.
 */
public data class Triple<out A, out B, out C>(
    public val first: A,
    public val second: B,
    public val third: C
) : Serializable {

    /**
     * Returns string representation of the [Triple] including its [first], [second] and [third] values.
     */
    public override fun toString(): String = "($first, $second, $third)"
}

/**
 * Converts this triple into a list.
 * @sample samples.misc.Tuples.tripleToList
 */
public fun <T> Triple<T, T, T>.toList(): List<T> = listOf(first, second, third)

/**
 * Transforms the first component of a [Triple] by applying the given [transform]
 * function.
 *  @sample samples.misc.Tuples.tripleMapFirst
 */
public inline fun <A, B, C, T> Triple<A, B, C>.mapFirst(transform: (A) -> T): Triple<T, B, C> {
    contract {
        callsInPlace(transform, InvocationKind.EXACTLY_ONCE)
    }
    return Triple(transform(first), second, third)
}

/**
 * Transforms the second component of a [Triple] by applying the given [transform]
 * function.
 *  @sample samples.misc.Tuples.tripleMapSecond
 */
public inline fun <A, B, C, T> Triple<A, B, C>.mapSecond(transform: (B) -> T): Triple<A, T, C> {
    contract {
        callsInPlace(transform, InvocationKind.EXACTLY_ONCE)
    }
    return Triple(first, transform(second), third)
}

/**
 * Transforms the third component of a [Triple] by applying the given [transform]
 * function.
 *  @sample samples.misc.Tuples.tripleMapThird
 */
public inline fun <A, B, C, T> Triple<A, B, C>.mapThird(transform: (C) -> T): Triple<A, B, T> {
    contract {
        callsInPlace(transform, InvocationKind.EXACTLY_ONCE)
    }
    return Triple(first, second, transform(third))
}
