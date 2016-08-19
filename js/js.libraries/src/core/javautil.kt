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

package java.util

@native
private val DEFAULT_INITIAL_CAPACITY = 16

@native
private val DEFAULT_LOAD_FACTOR = 0.75f

@library
public interface Comparator<T> {
    public fun compare(obj1: T, obj2: T): Int
}

public inline fun <T> Comparator(crossinline comparison: (T, T) -> Int): Comparator<T> = object : Comparator<T> {
    override fun compare(obj1: T, obj2: T): Int = comparison(obj1, obj2)
}


// in lack of type aliases
/*
public abstract class AbstractCollection<E> : kotlin.collections.AbstractCollection<E>()
public abstract class AbstractList<E> : kotlin.collections.AbstractList<E>()
public open class ArrayList<E>(capacity: Int = 0) : kotlin.collections.ArrayList<E>(capacity)
*/


@library
public open class HashSet<E>(
        initialCapacity: Int = DEFAULT_INITIAL_CAPACITY, loadFactor: Float = DEFAULT_LOAD_FACTOR
) : AbstractCollection<E>(), MutableSet<E> {
    override fun iterator(): MutableIterator<E> = noImpl
    override val size: Int get() = noImpl
    override fun equals(other: Any?): Boolean = noImpl

    override fun hashCode(): Int = noImpl
}

@library
public open class LinkedHashSet<E>(
        initialCapacity: Int = DEFAULT_INITIAL_CAPACITY, loadFactor: Float = DEFAULT_LOAD_FACTOR
) : HashSet<E>(initialCapacity, loadFactor), MutableSet<E> {
    override val size: Int get() = noImpl
}

@library
public open class HashMap<K, V>(initialCapacity: Int = DEFAULT_INITIAL_CAPACITY, loadFactor: Float = DEFAULT_LOAD_FACTOR) : MutableMap<K, V> {
    override val size: Int get() = noImpl
    override fun isEmpty(): Boolean = noImpl
    override fun get(key: K): V? = noImpl
    override fun containsKey(key: K): Boolean = noImpl
    override fun put(key: K, value: V): V? = noImpl
    override fun putAll(m: Map<out K, V>): Unit = noImpl
    override fun remove(key: K): V? = noImpl
    override fun clear(): Unit = noImpl
    override fun containsValue(value: V): Boolean = noImpl
    override val keys: MutableSet<K> get() = noImpl
    override val values: MutableCollection<V> get() = noImpl
    override val entries: MutableSet<MutableMap.MutableEntry<K, V>> get() = noImpl
}

@library
public open class LinkedHashMap<K, V>(
        initialCapacity: Int = DEFAULT_INITIAL_CAPACITY, loadFactor: Float = DEFAULT_LOAD_FACTOR, accessOrder: Boolean = false
) : HashMap<K, V>(initialCapacity, loadFactor)


@native
public class Date() {
    public fun getTime(): Int = noImpl
}
