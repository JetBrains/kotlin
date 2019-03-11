/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license 
 * that can be found in the license/LICENSE.txt file.
 */

package kotlin.collections


/**
 * Returns a [List] that wraps the original array.
 */
@SinceKotlin("1.3")
@ExperimentalUnsignedTypes
public actual fun UIntArray.asList(): List<UInt> {
    return object : AbstractList<UInt>(), RandomAccess {
        override val size: Int get() = this@asList.size
        override fun isEmpty(): Boolean = this@asList.isEmpty()
        override fun contains(element: UInt): Boolean = this@asList.contains(element)
        override fun get(index: Int): UInt = this@asList[index]
        override fun indexOf(element: UInt): Int = this@asList.indexOf(element)
        override fun lastIndexOf(element: UInt): Int = this@asList.lastIndexOf(element)
    }
}

/**
 * Returns a [List] that wraps the original array.
 */
@SinceKotlin("1.3")
@ExperimentalUnsignedTypes
public actual fun ULongArray.asList(): List<ULong> {
    return object : AbstractList<ULong>(), RandomAccess {
        override val size: Int get() = this@asList.size
        override fun isEmpty(): Boolean = this@asList.isEmpty()
        override fun contains(element: ULong): Boolean = this@asList.contains(element)
        override fun get(index: Int): ULong = this@asList[index]
        override fun indexOf(element: ULong): Int = this@asList.indexOf(element)
        override fun lastIndexOf(element: ULong): Int = this@asList.lastIndexOf(element)
    }
}

/**
 * Returns a [List] that wraps the original array.
 */
@SinceKotlin("1.3")
@ExperimentalUnsignedTypes
public actual fun UByteArray.asList(): List<UByte> {
    return object : AbstractList<UByte>(), RandomAccess {
        override val size: Int get() = this@asList.size
        override fun isEmpty(): Boolean = this@asList.isEmpty()
        override fun contains(element: UByte): Boolean = this@asList.contains(element)
        override fun get(index: Int): UByte = this@asList[index]
        override fun indexOf(element: UByte): Int = this@asList.indexOf(element)
        override fun lastIndexOf(element: UByte): Int = this@asList.lastIndexOf(element)
    }
}

/**
 * Returns a [List] that wraps the original array.
 */
@SinceKotlin("1.3")
@ExperimentalUnsignedTypes
public actual fun UShortArray.asList(): List<UShort> {
    return object : AbstractList<UShort>(), RandomAccess {
        override val size: Int get() = this@asList.size
        override fun isEmpty(): Boolean = this@asList.isEmpty()
        override fun contains(element: UShort): Boolean = this@asList.contains(element)
        override fun get(index: Int): UShort = this@asList[index]
        override fun indexOf(element: UShort): Int = this@asList.indexOf(element)
        override fun lastIndexOf(element: UShort): Int = this@asList.lastIndexOf(element)
    }
}
