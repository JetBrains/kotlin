/*
 * Copyright 2010-2017 JetBrains s.r.o.
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

import kotlin.comparisons.*

/**
 * Sorts elements in the list in-place according to their natural sort order.
 */
public actual fun <T : Comparable<T>> MutableList<T>.sort(): Unit = sortWith(Comparator<T> { a: T, b: T -> a.compareTo(b) })

/**
 * Sorts elements in the list in-place according to the order specified with [comparator].
 */
public actual fun <T> MutableList<T>.sortWith(comparator: Comparator<in T>): Unit {
    if (size > 1) {
        val it = listIterator()
        val sortedArray = @Suppress("TYPE_PARAMETER_AS_REIFIED") toTypedArray().apply { sortWith(comparator) }
        for (v in sortedArray) {
            it.next()
            it.set(v)
        }
    }
}
/**
 * Provides a skeletal implementation of the [MutableMap] interface.
 *
 * The implementor is required to implement [entries] property, which should return mutable set of map entries, and [put] function.
 *
 * @param K the type of map keys. The map is invariant on its key type.
 * @param V the type of map values. The map is invariant on its value type.
 */
@SinceKotlin("1.1")
actual abstract class AbstractMutableMap<K, V> protected actual constructor() : AbstractMap<K, V>(), MutableMap<K, V> {
    /**
     * Associates the specified [value] with the specified [key] in the map.
     *
     * This method is redeclared as abstract, because it's not implemented in the base class,
     * so it must be always overridden in the concrete mutable collection implementation.
     *
     * @return the previous value associated with the key, or `null` if the key was not present in the map.
     */
    actual abstract override fun put(key: K, value: V): V?

    /**
     * Returns a [Set] of all keys in this map.
     */
    abstract override val keys: MutableSet<K>

    /**
     * Returns a [Collection] of all values in this map.
     */
    abstract override val values: MutableCollection<V>
}

/**
 * Provides a skeletal implementation of the [MutableSet] interface.
 *
 * @param E the type of elements contained in the set. The set is invariant on its element type.
 */
@SinceKotlin("1.1")
actual abstract class AbstractMutableSet<E> protected actual constructor() : AbstractSet<E>(), MutableSet<E> {
    /**
     * Adds the specified element to the set.
     *
     * This method is redeclared as abstract, because it's not implemented in the base class,
     * so it must be always overridden in the concrete mutable collection implementation.
     *
     * @return `true` if the element has been added, `false` if the element is already contained in the set.
     */
    actual abstract override fun add(element: E): Boolean
}