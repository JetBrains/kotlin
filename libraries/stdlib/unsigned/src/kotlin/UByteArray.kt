/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license 
 * that can be found in the license/LICENSE.txt file.
 */

// Auto-generated file. DO NOT EDIT!

package kotlin

@Suppress("NON_PUBLIC_PRIMARY_CONSTRUCTOR_OF_INLINE_CLASS")
public inline class UByteArray internal constructor(private val storage: ByteArray) : Collection<UByte> {

    /** Returns the array element at the given [index]. This method can be called using the index operator. */
    public operator fun get(index: Int): UByte = storage[index].toUByte()

    /** Sets the element at the given [index] to the given [value]. This method can be called using the index operator. */
    public operator fun set(index: Int, value: UByte) {
        storage[index] = value.toByte()
    }

    /** Returns the number of elements in the array. */
    public override val size: Int get() = storage.size

    /** Creates an iterator over the elements of the array. */
    public override operator fun iterator(): UByteIterator = Iterator(storage)

    private class Iterator(private val array: ByteArray) : UByteIterator() {
        private var index = 0
        override fun hasNext() = index < array.size
        override fun nextUByte() = if (index < array.size) array[index++].toUByte() else throw NoSuchElementException(index.toString())
    }

    override fun contains(element: UByte): Boolean = storage.contains(element.toByte())

    override fun containsAll(elements: Collection<UByte>): Boolean = elements.all { storage.contains(it.toByte()) }

    override fun isEmpty(): Boolean = this.storage.size == 0
}

public /*inline*/ fun UByteArray(size: Int, init: (Int) -> UByte): UByteArray {
    return UByteArray(ByteArray(size) { index -> init(index).toByte() })
}

@Suppress("FORBIDDEN_VARARG_PARAMETER_TYPE")
public fun ubyteArrayOf(vararg elements: UByte): UByteArray {
    return UByteArray(elements.size) { index -> elements[index] }
}
