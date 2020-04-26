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
