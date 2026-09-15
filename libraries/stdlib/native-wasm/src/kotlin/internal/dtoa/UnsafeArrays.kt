/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:OptIn(ExperimentalUnsignedTypes::class)
@file:Suppress("NOTHING_TO_INLINE")

package kotlin.internal.dtoa

internal expect fun longArrayGetUnsafe(array: LongArray, index: Int): Long
internal expect fun longArraySetUnsafe(array: LongArray, index: Int, value: Long)
internal expect fun copyIntoUnsafe(source: LongArray, destination: LongArray, destinationOffset: Int, startIndex: Int, endIndex: Int)
internal expect fun doubleArrayGetUnsafe(array: DoubleArray, index: Int): Double
internal expect fun doubleArraySetUnsafe(array: DoubleArray, index: Int, value: Double)
internal expect fun intArrayGetUnsafe(array: IntArray, index: Int): Int
internal expect fun intArraySetUnsafe(array: IntArray, index: Int, value: Int)

internal value class ULongUnsafeArray(val array: ULongArray)

internal inline fun ULongArray.unsafe(): ULongUnsafeArray = ULongUnsafeArray(this)

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

internal inline fun DoubleArray.unsafe(): DoubleUnsafeArray = DoubleUnsafeArray(this)

internal inline operator fun DoubleUnsafeArray.get(index: Int): Double =
    doubleArrayGetUnsafe(array, index)

internal inline operator fun DoubleUnsafeArray.set(index: Int, value: Double): Unit =
    doubleArraySetUnsafe(array, index, value)

internal value class IntUnsafeArray(val array: IntArray)

internal inline fun IntArray.unsafe(): IntUnsafeArray = IntUnsafeArray(this)

internal inline operator fun IntUnsafeArray.get(index: Int): Int =
    intArrayGetUnsafe(array, index)

internal inline operator fun IntUnsafeArray.set(index: Int, value: Int): Unit =
    intArraySetUnsafe(array, index, value)
