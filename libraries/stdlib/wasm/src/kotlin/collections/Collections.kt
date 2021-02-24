/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.collections

actual interface RandomAccess

/** Returns the array if it's not `null`, or an empty array otherwise. */
actual inline fun <reified T> Array<out T>?.orEmpty(): Array<out T> = this ?: emptyArray<T>()


public actual inline fun <reified T> Collection<T>.toTypedArray(): Array<T> {
    val result = arrayOfNulls<T>(size)
    var index = 0
    for (element in this) result[index++] = element
    @Suppress("UNCHECKED_CAST")
    return result as Array<T>
}

@SinceKotlin("1.2")
actual fun <T> MutableList<T>.fill(value: T): Unit = TODO("Wasm stdlib: Collections")

@SinceKotlin("1.2")
actual fun <T> MutableList<T>.shuffle(): Unit = TODO("Wasm stdlib: Collections")

@SinceKotlin("1.2")
actual fun <T> Iterable<T>.shuffled(): List<T> = TODO("Wasm stdlib: Collections")

actual fun <T : Comparable<T>> MutableList<T>.sort(): Unit = TODO("Wasm stdlib: Collections")
actual fun <T> MutableList<T>.sortWith(comparator: Comparator<in T>): Unit = TODO("Wasm stdlib: Collections")


// from Grouping.kt
public actual fun <T, K> Grouping<T, K>.eachCount(): Map<K, Int> = TODO("Wasm stdlib: Collections")
// public actual inline fun <T, K> Grouping<T, K>.eachSumOf(valueSelector: (T) -> Int): Map<K, Int>

internal actual fun <K, V> Map<K, V>.toSingletonMapOrSelf(): Map<K, V> = TODO("Wasm stdlib: Collections")
internal actual fun <K, V> Map<out K, V>.toSingletonMap(): Map<K, V> = TODO("Wasm stdlib: Collections")
internal actual fun <T> Array<out T>.copyToArrayOfAny(isVarargs: Boolean): Array<out Any?> = TODO("Wasm stdlib: Collections")

@PublishedApi
@SinceKotlin("1.3")
internal actual fun checkIndexOverflow(index: Int): Int = TODO("Wasm stdlib: Collections")

@PublishedApi
@SinceKotlin("1.3")
internal actual fun checkCountOverflow(count: Int): Int = TODO("Wasm stdlib: Collections")

@PublishedApi
@SinceKotlin("1.3")
@ExperimentalStdlibApi
@kotlin.internal.InlineOnly
internal actual inline fun <E> buildListInternal(builderAction: MutableList<E>.() -> Unit): List<E> {
    return TODO("Wasm stdlib: Collections")
}

@PublishedApi
@SinceKotlin("1.3")
@ExperimentalStdlibApi
@kotlin.internal.InlineOnly
internal actual inline fun <E> buildListInternal(capacity: Int, builderAction: MutableList<E>.() -> Unit): List<E> {
    checkBuilderCapacity(capacity)
    return TODO("Wasm stdlib: Collections")
}


/**
 * Returns an immutable set containing only the specified object [element].
 */
public fun <T> setOf(element: T): Set<T> = hashSetOf(element)

@PublishedApi
@SinceKotlin("1.3")
@ExperimentalStdlibApi
@kotlin.internal.InlineOnly
internal actual inline fun <E> buildSetInternal(builderAction: MutableSet<E>.() -> Unit): Set<E> {
    return TODO("Wasm stdlib: Collections")
}

@PublishedApi
@SinceKotlin("1.3")
@ExperimentalStdlibApi
@kotlin.internal.InlineOnly
internal actual inline fun <E> buildSetInternal(capacity: Int, builderAction: MutableSet<E>.() -> Unit): Set<E> {
    return TODO("Wasm stdlib: Collections")
}


/**
 * Returns an immutable map, mapping only the specified key to the
 * specified value.
 */
public fun <K, V> mapOf(pair: Pair<K, V>): Map<K, V> = hashMapOf(pair)

@PublishedApi
@SinceKotlin("1.3")
@ExperimentalStdlibApi
@kotlin.internal.InlineOnly
internal actual inline fun <K, V> buildMapInternal(builderAction: MutableMap<K, V>.() -> Unit): Map<K, V> {
    return TODO("Wasm stdlib: Collections")
}

@PublishedApi
@SinceKotlin("1.3")
@ExperimentalStdlibApi
@kotlin.internal.InlineOnly
internal actual inline fun <K, V> buildMapInternal(capacity: Int, builderAction: MutableMap<K, V>.() -> Unit): Map<K, V> {
    return TODO("Wasm stdlib: Collections")
}