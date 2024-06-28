/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
package kotlin.collections

import kotlin.js.JsName

/**
 * Provides a skeletal implementation of the read-only [Collection] interface.
 *
 * @param E the type of elements contained in the collection. The collection is covariant in its element type.
 */
@SinceKotlin("1.1")
public abstract class AbstractCollection<out E> protected constructor() : Collection<E> {
    abstract override val size: Int
    abstract override fun iterator(): Iterator<E>

    override fun contains(element: @UnsafeVariance E): Boolean = any { it == element }

    override fun containsAll(elements: Collection<@UnsafeVariance E>): Boolean =
        elements.all { contains(it) } // use when js will support bound refs: elements.all(this::contains)

    override fun isEmpty(): Boolean = size == 0

    override fun toString(): String = joinToString(", ", "[", "]") {
        if (it === this) "(this Collection)" else it.toString()
    }

    /**
     * Returns new array of type `Array<Any?>` with the elements of this collection.
     */
    @JsName("toArray")
    protected open fun toArray(): Array<Any?> = collectionToArray(this)

    /**
     * Fills the provided [array] or creates new array of the same type
     * and fills it with the elements of this collection.
     *
     * If this collection doesn't fit in the provided [array],
     * a new array is created with the same array type and the size of this collection,
     * and filled with this collection elements.
     * Otherwise, the specified [array] is filled starting from index 0.
     * The value of the elements following the collection elements is unspecified.
     *
     * @return An array containing all elements of this collection.
     */
    protected open fun <T> toArray(array: Array<T>): Array<T> = collectionToArray(this, array)
}
