/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license 
 * that can be found in the license/LICENSE.txt file.
 */

// Auto-generated file. DO NOT EDIT!

package kotlin

@Suppress("NON_PUBLIC_PRIMARY_CONSTRUCTOR_OF_INLINE_CLASS")
public inline class UIntArray internal constructor(private val storage: IntArray) : Collection<UInt> {

    /** Returns the array element at the given [index]. This method can be called using the index operator. */
    public operator fun get(index: Int): UInt = storage[index].toUInt()

    /** Sets the element at the given [index] to the given [value]. This method can be called using the index operator. */
    public operator fun set(index: Int, value: UInt) {
        storage[index] = value.toInt()
    }

    /** Returns the number of elements in the array. */
    public override val size: Int get() = storage.size

    /** Creates an iterator over the elements of the array. */
    public override operator fun iterator(): UIntIterator = Iterator(storage)

    private class Iterator(private val array: IntArray) : UIntIterator() {
        private var index = 0
        override fun hasNext() = index < array.size
        override fun nextUInt() = if (index < array.size) array[index++].toUInt() else throw NoSuchElementException(index.toString())
    }

    override fun contains(element: UInt): Boolean = storage.contains(element.toInt())

    override fun containsAll(elements: Collection<UInt>): Boolean = elements.all { storage.contains(it.toInt()) }

    override fun isEmpty(): Boolean = this.storage.size == 0
}

public /*inline*/ fun UIntArray(size: Int, init: (Int) -> UInt): UIntArray {
    return UIntArray(IntArray(size) { index -> init(index).toInt() })
}

@Suppress("FORBIDDEN_VARARG_PARAMETER_TYPE")
public fun uintArrayOf(vararg elements: UInt): UIntArray {
    return UIntArray(elements.size) { index -> elements[index] }
}
