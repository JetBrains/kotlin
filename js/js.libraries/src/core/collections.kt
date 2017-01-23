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

/** Returns the array if it's not `null`, or an empty array otherwise. */
public inline fun <T> Array<out T>?.orEmpty(): Array<out T> = this ?: emptyArray<T>()

@Suppress("NON_PUBLIC_CALL_FROM_PUBLIC_INLINE")
public inline fun <T> Collection<T>.toTypedArray(): Array<T> = copyToArray(this)

@JsName("copyToArray")
internal fun <T> copyToArray(collection: Collection<T>): Array<T> {
    return if (collection.asDynamic().toArray !== undefined)
        collection.asDynamic().toArray().unsafeCast<Array<T>>()
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

@JsName("copyToExistingArrayImpl")
internal fun <T> copyToArrayImpl(collection: Collection<*>, array: Array<T>): Array<T> {
    if (array.size < collection.size)
        return copyToArrayImpl(collection).unsafeCast<Array<T>>()

    val iterator = collection.iterator()
    var index = 0
    while (iterator.hasNext()) {
        array[index++] = iterator.next().unsafeCast<T>()
    }
    if (index < array.size) {
        array[index] = null.unsafeCast<T>()
    }
    return array
}

@library("arrayToString")
internal fun arrayToString(array: Array<*>): String = definedExternally

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
    collectionsSort(this, naturalOrder())
}

/**
 * Sorts elements in the list in-place according to the order specified with [comparator].
 */
public fun <T> MutableList<T>.sortWith(comparator: Comparator<in T>): Unit {
    collectionsSort(this, comparator)
}

private fun <T> collectionsSort(list: MutableList<T>, comparator: Comparator<in T>) {
    if (list.size <= 1) return

    val array = copyToArray(list)

    array.asDynamic().sort(comparator.asDynamic().compare.bind(comparator))

    for (i in 0..array.size - 1) {
        list[i] = array[i]
    }
}
