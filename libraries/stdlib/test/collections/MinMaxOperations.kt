/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package test.collections

import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

sealed class MinMaxOperations<C, T>(
    val min: C.() -> T, val max: C.() -> T, val minOrNull: C.() -> T?, val maxOrNull: C.() -> T?,
) {
    object AInt : MinMaxOperations<IntArray, Int>({ min() }, { max() }, { minOrNull() }, { maxOrNull() })
    object ALong : MinMaxOperations<LongArray, Long>({ min() }, { max() }, { minOrNull() }, { maxOrNull() })
    object AShort : MinMaxOperations<ShortArray, Short>({ min() }, { max() }, { minOrNull() }, { maxOrNull() })
    object AByte : MinMaxOperations<ByteArray, Byte>({ min() }, { max() }, { minOrNull() }, { maxOrNull() })
    object AUInt : MinMaxOperations<UIntArray, UInt>({ min() }, { max() }, { minOrNull() }, { maxOrNull() })
    object AULong : MinMaxOperations<ULongArray, ULong>({ min() }, { max() }, { minOrNull() }, { maxOrNull() })
    object AUShort : MinMaxOperations<UShortArray, UShort>({ min() }, { max() }, { minOrNull() }, { maxOrNull() })
    object AUByte : MinMaxOperations<UByteArray, UByte>({ min() }, { max() }, { minOrNull() }, { maxOrNull() })
    object AFloat : MinMaxOperations<FloatArray, Float>({ min() }, { max() }, { minOrNull() }, { maxOrNull() })
    object ADouble : MinMaxOperations<DoubleArray, Double>({ min() }, { max() }, { minOrNull() }, { maxOrNull() })
    object AChar : MinMaxOperations<CharArray, Char>({ min() }, { max() }, { minOrNull() }, { maxOrNull() })
    class ArrayT<T : Comparable<T>> : MinMaxOperations<Array<T>, T>({ min() }, { max() }, { minOrNull() }, { maxOrNull() }) {
        companion object {
            fun <T : Comparable<T>> expectMinMax(min: T, max: T, elements: Array<T>) {
                ArrayT<T>().expectMinMax(min, max, elements)
            }
        }
    }
    class IterableT<T : Comparable<T>> : MinMaxOperations<Iterable<T>, T>({ min() }, { max() }, { minOrNull() }, { maxOrNull() })
    class SequenceT<T : Comparable<T>> : MinMaxOperations<Sequence<T>, T>({ min() }, { max() }, { minOrNull() }, { maxOrNull() })
    object CharSeq : MinMaxOperations<CharSequence, Char>({ min() }, { max() }, { minOrNull() }, { maxOrNull() })


    fun expectMinMax(min: T, max: T, elements: C) {
        assertEquals(min, elements.minOrNull())
        assertEquals(max, elements.maxOrNull())
        assertEquals(min, elements.min())
        assertEquals(max, elements.max())
    }

    fun expectMinMaxEmpty(empty: C) {
        assertNull(empty.minOrNull())
        assertNull(empty.maxOrNull())
        assertFailsWith<NoSuchElementException> { empty.min() }
        assertFailsWith<NoSuchElementException> { empty.max() }
    }
}


sealed class MinMaxOperationsWith<C, T>(
    val minWith: C.(Comparator<T>) -> T, val maxWith: C.(Comparator<T>) -> T, val minWithOrNull: C.(Comparator<T>) -> T?, val maxWithOrNull: C.(Comparator<T>) -> T?,
) {
    object AInt : MinMaxOperationsWith<IntArray, Int>(IntArray::minWith, IntArray::maxWith, IntArray::minWithOrNull, IntArray::maxWithOrNull)
    object ALong : MinMaxOperationsWith<LongArray, Long>(LongArray::minWith, LongArray::maxWith, LongArray::minWithOrNull, LongArray::maxWithOrNull)
    object AShort : MinMaxOperationsWith<ShortArray, Short>(ShortArray::minWith, ShortArray::maxWith, ShortArray::minWithOrNull, ShortArray::maxWithOrNull)
    object AByte : MinMaxOperationsWith<ByteArray, Byte>(ByteArray::minWith, ByteArray::maxWith, ByteArray::minWithOrNull, ByteArray::maxWithOrNull)
    object AUInt : MinMaxOperationsWith<UIntArray, UInt>(UIntArray::minWith, UIntArray::maxWith, UIntArray::minWithOrNull, UIntArray::maxWithOrNull)
    object AULong : MinMaxOperationsWith<ULongArray, ULong>(ULongArray::minWith, ULongArray::maxWith, ULongArray::minWithOrNull, ULongArray::maxWithOrNull)
    object AUShort : MinMaxOperationsWith<UShortArray, UShort>(UShortArray::minWith, UShortArray::maxWith, UShortArray::minWithOrNull, UShortArray::maxWithOrNull)
    object AUByte : MinMaxOperationsWith<UByteArray, UByte>(UByteArray::minWith, UByteArray::maxWith, UByteArray::minWithOrNull, UByteArray::maxWithOrNull)
    object AFloat : MinMaxOperationsWith<FloatArray, Float>(FloatArray::minWith, FloatArray::maxWith, FloatArray::minWithOrNull, FloatArray::maxWithOrNull)
    object ADouble : MinMaxOperationsWith<DoubleArray, Double>(DoubleArray::minWith, DoubleArray::maxWith, DoubleArray::minWithOrNull, DoubleArray::maxWithOrNull)
    object AChar : MinMaxOperationsWith<CharArray, Char>(CharArray::minWith, CharArray::maxWith, CharArray::minWithOrNull, CharArray::maxWithOrNull)
    class ArrayT<T> : MinMaxOperationsWith<Array<T>, T>(Array<T>::minWith, Array<T>::maxWith, Array<T>::minWithOrNull, Array<T>::maxWithOrNull) {
        companion object {
            fun <T> expectMinMaxWith(min: T, max: T, elements: Array<T>, comparator: Comparator<T>) {
                ArrayT<T>().expectMinMaxWith(min, max, elements, comparator)
            }
        }
    }
//    class IterableT<T : Comparable<T>> : MinMaxOperationsWith<Iterable<T>, T>({ min() }, { max() }, { minOrNull() }, { maxOrNull() })
//    class SequenceT<T : Comparable<T>> : MinMaxOperationsWith<Sequence<T>, T>({ min() }, { max() }, { minOrNull() }, { maxOrNull() })
//    object CharSeq : MinMaxOperationsWith<CharSequence, Char>({ min() }, { max() }, { minOrNull() }, { maxOrNull() })


    fun expectMinMaxWith(min: T, max: T, elements: C, comparator: Comparator<T>) {
        assertEquals(min, elements.minWithOrNull(comparator))
        assertEquals(max, elements.maxWithOrNull(comparator))
        assertEquals(min, elements.minWith(comparator))
        assertEquals(max, elements.maxWith(comparator))
    }

    fun expectMinMaxWithEmpty(empty: C, comparator: Comparator<T>) {
        assertNull(empty.minWithOrNull(comparator))
        assertNull(empty.maxWithOrNull(comparator))
        assertFailsWith<NoSuchElementException> { empty.minWith(comparator) }
        assertFailsWith<NoSuchElementException> { empty.maxWith(comparator) }
    }
}


