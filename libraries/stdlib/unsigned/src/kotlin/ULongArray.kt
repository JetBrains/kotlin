/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license 
 * that can be found in the license/LICENSE.txt file.
 */

// Auto-generated file. DO NOT EDIT!

package kotlin

@Suppress("NON_PUBLIC_PRIMARY_CONSTRUCTOR_OF_INLINE_CLASS")
public inline class ULongArray internal constructor(private val storage: LongArray) : Collection<ULong> {

    /** Returns the array element at the given [index]. This method can be called using the index operator. */
    public operator fun get(index: Int): ULong = storage[index].toULong()

    /** Sets the element at the given [index] to the given [value]. This method can be called using the index operator. */
    public operator fun set(index: Int, value: ULong) {
        storage[index] = value.toLong()
    }

    /** Returns the number of elements in the array. */
    public override val size: Int get() = storage.size

    /** Creates an iterator over the elements of the array. */
    public override operator fun iterator(): ULongIterator = Iterator(storage)

    private class Iterator(private val array: LongArray) : ULongIterator() {
        private var index = 0
        override fun hasNext() = index < array.size
        override fun nextULong() = if (index < array.size) array[index++].toULong() else throw NoSuchElementException(index.toString())
    }

    override fun contains(element: ULong): Boolean = storage.contains(element.toLong())

    override fun containsAll(elements: Collection<ULong>): Boolean = elements.all { storage.contains(it.toLong()) }

    override fun isEmpty(): Boolean = this.storage.size == 0
}

public /*inline*/ fun ULongArray(size: Int, init: (Int) -> ULong): ULongArray {
    return ULongArray(LongArray(size) { index -> init(index).toLong() })
}

@Suppress("FORBIDDEN_VARARG_PARAMETER_TYPE")
public fun ulongArrayOf(vararg elements: ULong): ULongArray {
    return ULongArray(elements.size) { index -> elements[index] }
}
