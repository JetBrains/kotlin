/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */
package kotlin.native

import kotlin.experimental.ExperimentalNativeApi
import kotlin.native.internal.escapeAnalysis.Escapes
import kotlin.native.internal.GCUnsafeCall

/**
 * Those operations allows to extract primitive values out of the [ByteArray] byte buffers.
 * Data is treated as if it was in Least-Significant-Byte first (little-endian) byte order.
 * If index is outside of array boundaries  - [IndexOutOfBoundsException] is thrown.
 */

/**
 * Gets [UByte] out of the [ByteArray] byte buffer at specified index [index]
 * @throws IndexOutOfBoundsException if [index] is outside of array boundaries.
 */
@ExperimentalNativeApi
public fun ByteArray.getUByteAt(index: Int): UByte = UByte(get(index))

/**
 * Gets [Char] out of the [ByteArray] byte buffer at specified index [index]
 * @throws IndexOutOfBoundsException if [index] is outside of array boundaries.
 */
@ExperimentalNativeApi
@GCUnsafeCall("Kotlin_ByteArray_getCharAt")
@Escapes.Nothing
public external fun ByteArray.getCharAt(index: Int): Char

/**
 * Gets [Short] out of the [ByteArray] byte buffer at specified index [index]
 * @throws IndexOutOfBoundsException if [index] is outside of array boundaries.
 */
@ExperimentalNativeApi
@GCUnsafeCall("Kotlin_ByteArray_getShortAt")
@Escapes.Nothing
public external fun ByteArray.getShortAt(index: Int): Short

/**
 * Gets [UShort] out of the [ByteArray] byte buffer at specified index [index]
 * @throws IndexOutOfBoundsException if [index] is outside of array boundaries.
 */
@ExperimentalNativeApi
@GCUnsafeCall("Kotlin_ByteArray_getShortAt")
@ExperimentalUnsignedTypes
@Escapes.Nothing
public external fun ByteArray.getUShortAt(index: Int): UShort

/**
 * Gets [Int] out of the [ByteArray] byte buffer at specified index [index]
 * @throws IndexOutOfBoundsException if [index] is outside of array boundaries.
 */
@ExperimentalNativeApi
@GCUnsafeCall("Kotlin_ByteArray_getIntAt")
@Escapes.Nothing
public external fun ByteArray.getIntAt(index: Int): Int

/**
 * Gets [UInt] out of the [ByteArray] byte buffer at specified index [index]
 * @throws IndexOutOfBoundsException if [index] is outside of array boundaries.
 */
@ExperimentalNativeApi
@GCUnsafeCall("Kotlin_ByteArray_getIntAt")
@ExperimentalUnsignedTypes
@Escapes.Nothing
public external fun ByteArray.getUIntAt(index: Int): UInt

/**
 * Gets [Long] out of the [ByteArray] byte buffer at specified index [index]
 * @throws IndexOutOfBoundsException if [index] is outside of array boundaries.
 */
@ExperimentalNativeApi
@GCUnsafeCall("Kotlin_ByteArray_getLongAt")
@Escapes.Nothing
public external fun ByteArray.getLongAt(index: Int): Long

/**
 * Gets [ULong] out of the [ByteArray] byte buffer at specified index [index]
 * @throws IndexOutOfBoundsException if [index] is outside of array boundaries.
 */
@ExperimentalNativeApi
@GCUnsafeCall("Kotlin_ByteArray_getLongAt")
@ExperimentalUnsignedTypes
@Escapes.Nothing
public external fun ByteArray.getULongAt(index: Int): ULong

/**
 * Gets [Float] out of the [ByteArray] byte buffer at specified index [index]
 * @throws IndexOutOfBoundsException if [index] is outside of array boundaries.
 */
@ExperimentalNativeApi
@GCUnsafeCall("Kotlin_ByteArray_getFloatAt")
@Escapes.Nothing
public external fun ByteArray.getFloatAt(index: Int): Float

/**
 * Gets [Double] out of the [ByteArray] byte buffer at specified index [index]
 * @throws IndexOutOfBoundsException if [index] is outside of array boundaries.
 */
@ExperimentalNativeApi
@GCUnsafeCall("Kotlin_ByteArray_getDoubleAt")
@Escapes.Nothing
public external fun ByteArray.getDoubleAt(index: Int): Double

/**
 * Sets [UByte] out of the [ByteArray] byte buffer at specified index [index]
 * @throws IndexOutOfBoundsException if [index] is outside of array boundaries.
 */
@ExperimentalNativeApi
@GCUnsafeCall("Kotlin_ByteArray_set")
@Escapes.Nothing
public external fun ByteArray.setUByteAt(index: Int, value: UByte)

/**
 * Sets [Char] out of the [ByteArray] byte buffer at specified index [index]
 * @throws IndexOutOfBoundsException if [index] is outside of array boundaries.
 */
@ExperimentalNativeApi
@GCUnsafeCall("Kotlin_ByteArray_setCharAt")
@Escapes.Nothing
public external fun ByteArray.setCharAt(index: Int, value: Char)

/**
 * Sets [Short] out of the [ByteArray] byte buffer at specified index [index]
 * @throws IndexOutOfBoundsException if [index] is outside of array boundaries.
 */
@ExperimentalNativeApi
@GCUnsafeCall("Kotlin_ByteArray_setShortAt")
@Escapes.Nothing
public external fun ByteArray.setShortAt(index: Int, value: Short)

/**
 * Sets [UShort] out of the [ByteArray] byte buffer at specified index [index]
 * @throws IndexOutOfBoundsException if [index] is outside of array boundaries.
 */
@ExperimentalNativeApi
@GCUnsafeCall("Kotlin_ByteArray_setShortAt")
@ExperimentalUnsignedTypes
@Escapes.Nothing
public external fun ByteArray.setUShortAt(index: Int, value: UShort)

/**
 * Sets [Int] out of the [ByteArray] byte buffer at specified index [index]
 * @throws IndexOutOfBoundsException if [index] is outside of array boundaries.
 */
@ExperimentalNativeApi
@GCUnsafeCall("Kotlin_ByteArray_setIntAt")
@Escapes.Nothing
public external fun ByteArray.setIntAt(index: Int, value: Int)

/**
 * Sets [UInt] out of the [ByteArray] byte buffer at specified index [index]
 * @throws IndexOutOfBoundsException if [index] is outside of array boundaries.
 */
@ExperimentalNativeApi
@GCUnsafeCall("Kotlin_ByteArray_setIntAt")
@Escapes.Nothing
public external fun ByteArray.setUIntAt(index: Int, value: UInt)

/**
 * Sets [Long] out of the [ByteArray] byte buffer at specified index [index]
 * @throws IndexOutOfBoundsException if [index] is outside of array boundaries.
 */
@ExperimentalNativeApi
@GCUnsafeCall("Kotlin_ByteArray_setLongAt")
@Escapes.Nothing
public external fun ByteArray.setLongAt(index: Int, value: Long)

/**
 * Sets [ULong] out of the [ByteArray] byte buffer at specified index [index]
 * @throws IndexOutOfBoundsException if [index] is outside of array boundaries.
 */
@ExperimentalNativeApi
@GCUnsafeCall("Kotlin_ByteArray_setLongAt")
@ExperimentalUnsignedTypes
@Escapes.Nothing
public external fun ByteArray.setULongAt(index: Int, value: ULong)

/**
 * Sets [Float] out of the [ByteArray] byte buffer at specified index [index]
 * @throws IndexOutOfBoundsException if [index] is outside of array boundaries.
 */
@ExperimentalNativeApi
@GCUnsafeCall("Kotlin_ByteArray_setFloatAt")
@Escapes.Nothing
public external fun ByteArray.setFloatAt(index: Int, value: Float)

/**
 * Sets [Double] out of the [ByteArray] byte buffer at specified index [index]
 * @throws IndexOutOfBoundsException if [index] is outside of array boundaries.
 */
@ExperimentalNativeApi
@GCUnsafeCall("Kotlin_ByteArray_setDoubleAt")
@Escapes.Nothing
public external fun ByteArray.setDoubleAt(index: Int, value: Double)
