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
