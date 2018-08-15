/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license 
 * that can be found in the license/LICENSE.txt file.
 */

// Auto-generated file. DO NOT EDIT!

package kotlin

@SinceKotlin("1.3")
@ExperimentalUnsignedTypes
public inline class UShortArray
@Suppress("NON_PUBLIC_PRIMARY_CONSTRUCTOR_OF_INLINE_CLASS")
@PublishedApi
internal constructor(@PublishedApi internal val storage: ShortArray) : Collection<UShort> {

    /** Creates a new array of the specified [size], with all elements initialized to zero. */
    public constructor(size: Int) : this(ShortArray(size))

    /** Returns the array element at the given [index]. This method can be called using the index operator. */
    public operator fun get(index: Int): UShort = storage[index].toUShort()

    /** Sets the element at the given [index] to the given [value]. This method can be called using the index operator. */
    public operator fun set(index: Int, value: UShort) {
        storage[index] = value.toShort()
    }

    /** Returns the number of elements in the array. */
    public override val size: Int get() = storage.size

    /** Creates an iterator over the elements of the array. */
    public override operator fun iterator(): UShortIterator = Iterator(storage)

    private class Iterator(private val array: ShortArray) : UShortIterator() {
        private var index = 0
        override fun hasNext() = index < array.size
        override fun nextUShort() = if (index < array.size) array[index++].toUShort() else throw NoSuchElementException(index.toString())
    }

    override fun contains(element: UShort): Boolean = storage.contains(element.toShort())

    override fun containsAll(elements: Collection<UShort>): Boolean = elements.all { storage.contains(it.toShort()) }

    override fun isEmpty(): Boolean = this.storage.size == 0
}

@SinceKotlin("1.3")
@ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun UShortArray(size: Int, init: (Int) -> UShort): UShortArray {
    return UShortArray(ShortArray(size) { index -> init(index).toShort() })
}

@SinceKotlin("1.3")
@ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun ushortArrayOf(vararg elements: UShort): UShortArray = elements
