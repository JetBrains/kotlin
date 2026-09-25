/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:OptIn(ExperimentalUnsignedTypes::class)
@file:Suppress("NOTHING_TO_INLINE")

package kotlin.internal.dtoa

/**
 * Unchecked array access functions and wrapper classes used by floating-point parser and stringifier routines (`dtoa`).
 *
 * Why they exist:
 * These functions and wrapper classes eliminate array range checks, making floating-point string conversion and parsing
 * as fast as the original C implementation. With standard Kotlin bounds checks, performance degrades by about 10% in these hot paths.
 *
 * Trade-offs:
 * - Performance benefit: Recovers ~10% performance overhead by avoiding repeated range checks during numeric parsing and big-integer arithmetic.
 * - Safety trade-off: Out-of-bounds indexing will result in undefined behavior or memory corruption rather than an [IndexOutOfBoundsException].
 *   Therefore, these functions and classes are strictly internal and must only be used when index bounds are mathematically guaranteed.
 */

internal expect fun longArrayGetUnsafe(array: LongArray, index: Int): Long
internal expect fun longArraySetUnsafe(array: LongArray, index: Int, value: Long)
internal expect fun copyIntoUnsafe(source: LongArray, destination: LongArray, destinationOffset: Int, startIndex: Int, endIndex: Int)
internal expect fun doubleArrayGetUnsafe(array: DoubleArray, index: Int): Double
internal expect fun doubleArraySetUnsafe(array: DoubleArray, index: Int, value: Double)
internal expect fun intArrayGetUnsafe(array: IntArray, index: Int): Int
internal expect fun intArraySetUnsafe(array: IntArray, index: Int, value: Int)

internal value class ULongUnsafeArray(val array: ULongArray)

internal inline fun ULongUnsafeArray(size: Int): ULongUnsafeArray = ULongUnsafeArray(ULongArray(size))

internal inline operator fun ULongUnsafeArray.get(index: Int): ULong =
    longArrayGetUnsafe(array.storage, index).toULong()

internal inline operator fun ULongUnsafeArray.set(index: Int, value: ULong):Unit =
    longArraySetUnsafe(array.storage, index, value.toLong())

internal inline fun ULongUnsafeArray.copyInto(destination: ULongUnsafeArray, destinationOffset: Int = 0, startIndex: Int = 0, endIndex: Int = array.size): ULongUnsafeArray {
    copyIntoUnsafe(array.storage, destination.array.storage, startIndex, destinationOffset, endIndex)
    return destination
}

internal inline fun ULongUnsafeArray.fill(element: ULong, fromIndex: Int = 0, toIndex: Int = array.size) {
    for (index in fromIndex until toIndex) {
        this[index] = element
    }
}

internal value class DoubleUnsafeArray(val array: DoubleArray)

internal inline fun doubleUnsafeArrayOf(vararg values: Double): DoubleUnsafeArray = DoubleUnsafeArray(values)

internal inline operator fun DoubleUnsafeArray.get(index: Int): Double =
    doubleArrayGetUnsafe(array, index)

internal inline operator fun DoubleUnsafeArray.set(index: Int, value: Double): Unit =
    doubleArraySetUnsafe(array, index, value)

internal value class IntUnsafeArray(val array: IntArray)

internal inline fun intUnsafeArrayOf(vararg values: Int): IntUnsafeArray = IntUnsafeArray(values)

internal inline operator fun IntUnsafeArray.get(index: Int): Int =
    intArrayGetUnsafe(array, index)

internal inline operator fun IntUnsafeArray.set(index: Int, value: Int): Unit =
    intArraySetUnsafe(array, index, value)
