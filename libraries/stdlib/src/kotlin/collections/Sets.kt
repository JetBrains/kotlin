/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:kotlin.jvm.JvmMultifileClass
@file:kotlin.jvm.JvmName("SetsKt")
@file:UseExperimental(kotlin.experimental.ExperimentalTypeInference::class)

package kotlin.collections

import kotlin.contracts.*

internal object EmptySet : Set<Nothing>, Serializable {
    private const val serialVersionUID: Long = 3406603774387020532

    override fun equals(other: Any?): Boolean = other is Set<*> && other.isEmpty()
    override fun hashCode(): Int = 0
    override fun toString(): String = "[]"

    override val size: Int get() = 0
    override fun isEmpty(): Boolean = true
    override fun contains(element: Nothing): Boolean = false
    override fun containsAll(elements: Collection<Nothing>): Boolean = elements.isEmpty()

    override fun iterator(): Iterator<Nothing> = EmptyIterator

    private fun readResolve(): Any = EmptySet
}


/**
 * Returns an empty read-only set.  The returned set is serializable (JVM).
 * @sample samples.collections.Collections.Sets.emptyReadOnlySet
 */
public fun <T> emptySet(): Set<T> = EmptySet

/**
 * Returns a new read-only set with the given elements.
 * Elements of the set are iterated in the order they were specified.
 * The returned set is serializable (JVM).
 * @sample samples.collections.Collections.Sets.readOnlySet
 */
public fun <T> setOf(vararg elements: T): Set<T> = if (elements.size > 0) elements.toSet() else emptySet()

/**
 * Returns an empty read-only set.  The returned set is serializable (JVM).
 * @sample samples.collections.Collections.Sets.emptyReadOnlySet
 */
@kotlin.internal.InlineOnly
public inline fun <T> setOf(): Set<T> = emptySet()

/**
 * Returns an empty new [MutableSet].
 *
 * The returned set preserves the element iteration order.
 * @sample samples.collections.Collections.Sets.emptyMutableSet
 */
@SinceKotlin("1.1")
@kotlin.internal.InlineOnly
public inline fun <T> mutableSetOf(): MutableSet<T> = LinkedHashSet()

/**
 * Returns a new [MutableSet] with the given elements.
 * Elements of the set are iterated in the order they were specified.
 * @sample samples.collections.Collections.Sets.mutableSet
 */
public fun <T> mutableSetOf(vararg elements: T): MutableSet<T> = elements.toCollection(LinkedHashSet(mapCapacity(elements.size)))

/** Returns an empty new [HashSet]. */
@SinceKotlin("1.1")
@kotlin.internal.InlineOnly
public inline fun <T> hashSetOf(): HashSet<T> = HashSet()

/** Returns a new [HashSet] with the given elements. */
public fun <T> hashSetOf(vararg elements: T): HashSet<T> = elements.toCollection(HashSet(mapCapacity(elements.size)))

/**
 * Returns an empty new [LinkedHashSet].
 * @sample samples.collections.Collections.Sets.emptyLinkedHashSet
 */
@SinceKotlin("1.1")
@kotlin.internal.InlineOnly
public inline fun <T> linkedSetOf(): LinkedHashSet<T> = LinkedHashSet()

/**
 * Returns a new [LinkedHashSet] with the given elements.
 * Elements of the set are iterated in the order they were specified.
 * @sample samples.collections.Collections.Sets.linkedHashSet
 */
public fun <T> linkedSetOf(vararg elements: T): LinkedHashSet<T> = elements.toCollection(LinkedHashSet(mapCapacity(elements.size)))

/**
 * Build a new read-only [Set] with the [elements][E] from the [builderAction] while preserving the insertion order.
 *
 * @sample samples.collections.Builders.Sets.buildSetSample
 */
@SinceKotlin("1.3")
@ExperimentalStdlibApi
@kotlin.internal.InlineOnly
public inline fun <E> buildSet(@BuilderInference builderAction: MutableSet<E>.() -> Unit): Set<E> {
    contract { callsInPlace(builderAction, InvocationKind.EXACTLY_ONCE) }
    return LinkedHashSet<E>().apply(builderAction)
}

/**
 * Build a new read-only [Set] with the given [expectedSize] and [elements][E] from the [builderAction] while preserving the insertion
 * order.
 *
 * @sample samples.collections.Builders.Sets.buildSetSample
 * @throws IllegalArgumentException if the given [expectedSize] is negative.
 */
@SinceKotlin("1.3")
@ExperimentalStdlibApi
@kotlin.internal.InlineOnly
public inline fun <E> buildSet(expectedSize: Int, @BuilderInference builderAction: MutableSet<E>.() -> Unit): Set<E> {
    contract { callsInPlace(builderAction, InvocationKind.EXACTLY_ONCE) }
    checkBuilderCapacity(expectedSize)
    return LinkedHashSet<E>(mapCapacity(expectedSize)).apply(builderAction)
}


/** Returns this Set if it's not `null` and the empty set otherwise. */
@kotlin.internal.InlineOnly
public inline fun <T> Set<T>?.orEmpty(): Set<T> = this ?: emptySet()

internal fun <T> Set<T>.optimizeReadOnlySet() = when (size) {
    0 -> emptySet()
    1 -> setOf(iterator().next())
    else -> this
}
