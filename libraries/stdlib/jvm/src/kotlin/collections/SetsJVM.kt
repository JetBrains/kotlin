/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:kotlin.jvm.JvmMultifileClass
@file:kotlin.jvm.JvmName("SetsKt")

package kotlin.collections

import kotlin.collections.builders.SetBuilder

/**
 * Returns a new read-only set containing only the specified object [element].
 *
 * The returned set is serializable.
 *
 * @sample samples.collections.Collections.Sets.singletonReadOnlySet
 */
public actual fun <T> setOf(element: T): Set<T> = java.util.Collections.singleton(element)

@PublishedApi
@SinceKotlin("1.3")
@kotlin.internal.InlineOnly
internal actual inline fun <E> buildSetInternal(builderAction: MutableSet<E>.() -> Unit): Set<E> {
    return build(createSetBuilder<E>().apply(builderAction))
}

@PublishedApi
@SinceKotlin("1.3")
@kotlin.internal.InlineOnly
internal actual inline fun <E> buildSetInternal(capacity: Int, builderAction: MutableSet<E>.() -> Unit): Set<E> {
    return build(createSetBuilder<E>(capacity).apply(builderAction))
}

@PublishedApi
@SinceKotlin("1.3")
internal fun <E> createSetBuilder(): MutableSet<E> {
    return SetBuilder()
}

@PublishedApi
@SinceKotlin("1.3")
internal fun <E> createSetBuilder(capacity: Int): MutableSet<E> {
    return SetBuilder(capacity)
}

@PublishedApi
@SinceKotlin("1.3")
internal fun <E> build(builder: MutableSet<E>): Set<E> {
    return (builder as SetBuilder<E>).build()
}


/**
 * Returns a new [java.util.SortedSet] with the given elements.
 */
public fun <T> sortedSetOf(vararg elements: T): java.util.TreeSet<T> = elements.toCollection(java.util.TreeSet<T>())

/**
 * Returns a new [java.util.SortedSet] with the given [comparator] and elements.
 */
public fun <T> sortedSetOf(comparator: Comparator<in T>, vararg elements: T): java.util.TreeSet<T> = elements.toCollection(java.util.TreeSet<T>(comparator))

