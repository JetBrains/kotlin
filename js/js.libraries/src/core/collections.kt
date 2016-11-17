/*
 * Copyright 2010-2016 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package kotlin.collections

import kotlin.comparisons.naturalOrder

@Suppress("NON_PUBLIC_CALL_FROM_PUBLIC_INLINE")
public inline fun <reified T> Collection<T>.toTypedArray(): Array<T> = copyToArray(this)

@JsName("copyToArray")
internal fun <T> copyToArray(collection: Collection<T>): Array<T> {
    return if (collection.asDynamic().toArray !== undefined)
        collection.asDynamic().toArray()
    else
        copyToArrayImpl(collection).unsafeCast<Array<T>>()
}

@JsName("copyToArrayImpl")
internal fun copyToArrayImpl(collection: Collection<*>): Array<Any?> {
    val array = emptyArray<Any?>()
    val iterator = collection.iterator()
    while (iterator.hasNext())
        array.asDynamic().push(iterator.next())
    return array
}

@library("arrayToString")
internal fun arrayToString(array: Array<*>): String = noImpl

/**
 * Returns an immutable list containing only the specified object [element].
 */
public fun <T> listOf(element: T): List<T> = arrayListOf(element)

/**
 * Returns an immutable set containing only the specified object [element].
 */
public fun <T> setOf(element: T): Set<T> = hashSetOf(element)

/**
 * Returns an immutable map, mapping only the specified key to the
 * specified value.
 */
public fun <K, V> mapOf(pair: Pair<K, V>): Map<K, V> = hashMapOf(pair)

/**
 * Sorts elements in the list in-place according to their natural sort order.
 */
public fun <T : Comparable<T>> MutableList<T>.sort(): Unit {
    if (size > 1) collectionsSort(this, naturalOrder())
}

/**
 * Sorts elements in the list in-place according to the order specified with [comparator].
 */
public fun <T> MutableList<T>.sortWith(comparator: Comparator<in T>): Unit {
    if (size > 1) collectionsSort(this, comparator)
}

@library("collectionsSort")
private fun <T> collectionsSort(list: MutableList<T>, comparator: Comparator<in T>): Unit = noImpl


/**
 * Reverses elements in the list in-place.
 */
public fun <T> MutableList<T>.reverse(): Unit {
    val size = this.size
    for (i in 0..(size / 2) - 1) {
        val i2 = size - i - 1
        val tmp = this[i]
        this[i] = this[i2]
        this[i2] = tmp
    }
}
