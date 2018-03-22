/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

@file:kotlin.jvm.JvmMultifileClass
@file:kotlin.jvm.JvmName("CollectionsKt")

package kotlin.collections


@Deprecated("Use sortWith(comparator) instead.", ReplaceWith("this.sortWith(comparator)"), level = DeprecationLevel.ERROR)
@JvmVersion
@kotlin.internal.InlineOnly
@Suppress("UNUSED_PARAMETER")
public inline fun <T> MutableList<T>.sort(comparator: Comparator<in T>): Unit = throw NotImplementedError()

@Deprecated("Use sortWith(Comparator(comparison)) instead.", ReplaceWith("this.sortWith(Comparator(comparison))"), level = DeprecationLevel.ERROR)
@JvmVersion
@kotlin.internal.InlineOnly
@Suppress("UNUSED_PARAMETER")
public inline fun <T> MutableList<T>.sort(comparison: (T, T) -> Int): Unit = throw NotImplementedError()


/**
 * Sorts elements in the list in-place according to their natural sort order.
 */
@kotlin.jvm.JvmVersion
public fun <T : Comparable<T>> MutableList<T>.sort(): Unit {
    if (size > 1) java.util.Collections.sort(this)
}

/**
 * Sorts elements in the list in-place according to the order specified with [comparator].
 */
@kotlin.jvm.JvmVersion
public fun <T> MutableList<T>.sortWith(comparator: Comparator<in T>): Unit {
    if (size > 1) java.util.Collections.sort(this, comparator)
}

/**
 * Fills the list with the provided [value].
 *
 * Each element in the list gets replaced with the [value].
 */
@kotlin.jvm.JvmVersion
@kotlin.internal.InlineOnly
@SinceKotlin("1.2")
public inline fun <T> MutableList<T>.fill(value: T) {
    java.util.Collections.fill(this, value)
}


/**
 * Randomly shuffles elements in this mutable list.
 */
@kotlin.jvm.JvmVersion
@kotlin.internal.InlineOnly
@SinceKotlin("1.2")
public inline fun <T> MutableList<T>.shuffle() {
    java.util.Collections.shuffle(this)
}

/**
 * Randomly shuffles elements in this mutable list using the specified [random] instance as the source of randomness.
 */
@kotlin.jvm.JvmVersion
@kotlin.internal.InlineOnly
@SinceKotlin("1.2")
public inline fun <T> MutableList<T>.shuffle(random: java.util.Random) {
    java.util.Collections.shuffle(this, random)
}

/**
 * Returns a new list with the elements of this list randomly shuffled.
 */
@kotlin.jvm.JvmVersion
@SinceKotlin("1.2")
public fun <T> Iterable<T>.shuffled(): List<T> = toMutableList().apply { shuffle() }

/**
 * Returns a new list with the elements of this list randomly shuffled
 * using the specified [random] instance as the source of randomness.
 */
@kotlin.jvm.JvmVersion
@SinceKotlin("1.2")
public fun <T> Iterable<T>.shuffled(random: java.util.Random): List<T> = toMutableList().apply { shuffle(random) }
