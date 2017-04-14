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

/**
 * Provides a skeletal implementation of the read-only [Collection] interface.
 *
 * @param E the type of elements contained in the collection. The collection is covariant on its element type.
 */
@SinceKotlin("1.1")
public abstract class AbstractCollection<out E> protected constructor() : Collection<E> {
    abstract override val size: Int
    abstract override fun iterator(): Iterator<E>

    override fun contains(element: @UnsafeVariance E): Boolean = any { it == element }

    override fun containsAll(elements: Collection<@UnsafeVariance E>): Boolean =
            elements.all { contains(it) }

    override fun isEmpty(): Boolean = size == 0


    override fun toString(): String = joinToString(", ", "[", "]") {
        if (it === this) "(this Collection)" else it.toString()
    }

    /**
     * Returns new array of type `Array<Any?>` with the elements of this collection.
     */
    protected open fun toArray(): Array<Any?> = collectionToArray(this)

    /**
     * Fills the provided [array] or creates new array of the same type
     * and fills it with the elements of this collection.
     */
    protected open fun <T> toArray(array: Array<T>): Array<T> = collectionToArray(this, array)
}

public abstract class AbstractMutableCollection<E> protected constructor(): MutableCollection<E>, AbstractCollection<E>() {

    // Bulk Modification Operations
    /**
     * Adds all of the elements in the specified collection to this collection.
     *
     * @return `true` if any of the specified elements was added to the collection, `false` if the collection was not modified.
     */
    override public fun addAll(elements: Collection<E>): Boolean {
        var changed = false
        for (v in elements) {
            if(add(v)) changed = true
        }
        return changed
    }

    /**
     * Removes all of this collection's elements that are also contained in the specified collection.
     *
     * @return `true` if any of the specified elements was removed from the collection, `false` if the collection was not modified.
     */
    override public fun removeAll(elements: Collection<E>): Boolean = removeAll(elements, true)

    /**
     * Retains only the elements in this collection that are contained in the specified collection.
     *
     * @return `true` if any element was removed from the collection, `false` if the collection was not modified.
     */
    override public fun retainAll(elements: Collection<E>): Boolean = removeAll(elements, false)

    /**
     * Removes the elements in this collection that are contained (if [contained] == true) or
     * not contained (if [contained] == false) in [elements].
     *
     * @return `true` if the collection has been modified and `false` otherwise.
     */
    protected fun removeAll(elements: Collection<E>, contained: Boolean): Boolean {
        val it = iterator()
        var changed = false
        while (it.hasNext()) {
            if (elements.contains(it.next()) == contained) {
                it.remove()
                changed = true
            }
        }
        return changed
    }
}
