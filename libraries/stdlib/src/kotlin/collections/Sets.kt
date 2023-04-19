/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:kotlin.jvm.JvmMultifileClass
@file:kotlin.jvm.JvmName("SetsKt")
@file:OptIn(kotlin.experimental.ExperimentalTypeInference::class)

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
 * Returns a new read-only set containing only the specified object [element].
 *
 * The returned set is serializable (JVM).
 *
 * @sample samples.collections.Collections.Sets.singletonReadOnlySet
 */
@SinceKotlin("1.9")
public expect fun <T> setOf(element: T): Set<T>

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
 * Returns a new read-only set either with single given element, if it is not null, or empty set if the element is null.
 * The returned set is serializable (JVM).
 * @sample samples.collections.Collections.Sets.setOfNotNull
 */
@SinceKotlin("1.4")
public fun <T : Any> setOfNotNull(element: T?): Set<T> = if (element != null) setOf(element) else emptySet()

/**
 * Returns a new read-only set only with those given elements, that are not null.
 * Elements of the set are iterated in the order they were specified.
 * The returned set is serializable (JVM).
 * @sample samples.collections.Collections.Sets.setOfNotNull
 */
@SinceKotlin("1.4")
public fun <T : Any> setOfNotNull(vararg elements: T?): Set<T> {
    return elements.filterNotNullTo(LinkedHashSet())
}

/**
 * Builds a new read-only [Set] by populating a [MutableSet] using the given [builderAction]
 * and returning a read-only set with the same elements.
 *
 * The set passed as a receiver to the [builderAction] is valid only inside that function.
 * Using it outside of the function produces an unspecified behavior.
 *
 * Elements of the set are iterated in the order they were added by the [builderAction].
 *
 * The returned set is serializable (JVM).
 *
 * @sample samples.collections.Builders.Sets.buildSetSample
 */
@SinceKotlin("1.6")
@WasExperimental(ExperimentalStdlibApi::class)
@kotlin.internal.InlineOnly
@Suppress("DEPRECATION")
public inline fun <E> buildSet(@BuilderInference builderAction: MutableSet<E>.() -> Unit): Set<E> {
    contract { callsInPlace(builderAction, InvocationKind.EXACTLY_ONCE) }
    return buildSetInternal(builderAction)
}

@PublishedApi
@SinceKotlin("1.3")
@kotlin.internal.InlineOnly
internal expect inline fun <E> buildSetInternal(builderAction: MutableSet<E>.() -> Unit): Set<E>

/**
 * Builds a new read-only [Set] by populating a [MutableSet] using the given [builderAction]
 * and returning a read-only set with the same elements.
 *
 * The set passed as a receiver to the [builderAction] is valid only inside that function.
 * Using it outside of the function produces an unspecified behavior.
 *
 * [capacity] is used to hint the expected number of elements added in the [builderAction].
 *
 * Elements of the set are iterated in the order they were added by the [builderAction].
 *
 * The returned set is serializable (JVM).
 *
 * @throws IllegalArgumentException if the given [capacity] is negative.
 *
 * @sample samples.collections.Builders.Sets.buildSetSample
 */
@SinceKotlin("1.6")
@WasExperimental(ExperimentalStdlibApi::class)
@kotlin.internal.InlineOnly
@Suppress("DEPRECATION")
public inline fun <E> buildSet(capacity: Int, @BuilderInference builderAction: MutableSet<E>.() -> Unit): Set<E> {
    contract { callsInPlace(builderAction, InvocationKind.EXACTLY_ONCE) }
    return buildSetInternal(capacity, builderAction)
}

@PublishedApi
@SinceKotlin("1.3")
@kotlin.internal.InlineOnly
internal expect inline fun <E> buildSetInternal(capacity: Int, builderAction: MutableSet<E>.() -> Unit): Set<E>


/** Returns this Set if it's not `null` and the empty set otherwise. */
@kotlin.internal.InlineOnly
public inline fun <T> Set<T>?.orEmpty(): Set<T> = this ?: emptySet()

internal fun <T> Set<T>.optimizeReadOnlySet() = when (size) {
    0 -> emptySet()
    1 -> setOf(iterator().next())
    else -> this
}
