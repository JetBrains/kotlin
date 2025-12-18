/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin

import kotlin.native.internal.*
import kotlin.native.internal.escapeAnalysis.Escapes
import kotlin.native.internal.escapeAnalysis.PointsTo

/**
 * Returns an array of objects of the given type with the given [size], initialized with null values.
 *
 * @throws RuntimeException if the specified [size] is negative.
 */
@Suppress("NOTHING_TO_INLINE")
public actual inline fun <T> arrayOfNulls(size: Int): Array<T?> =
        @Suppress("NON_PUBLIC_CALL_FROM_PUBLIC_INLINE")
        arrayOfUninitializedElements<T?>(size)

/**
 * Returns an array containing the specified elements.
 *
 * @sample samples.collections.Arrays.Constructors.arrayOfSample
 */
@TypedIntrinsic(IntrinsicType.IDENTITY)
@PointsTo(0x00, 0x01) // ret -> elements
@Suppress("NOTHING_TO_INLINE")
public actual external inline fun <T> arrayOf(vararg elements: T): Array<T>

@GCUnsafeCall("Kotlin_emptyArray")
// The return value is statically allocated and immutable;
// we can treat it as non-escaping
@Escapes.Nothing
public actual external fun <T> emptyArray(): Array<T>

/**
 * Returns an array containing the specified [Double] numbers.
 *
 * @sample samples.collections.Arrays.Constructors.doubleArrayOfSample
 */
@Suppress("NOTHING_TO_INLINE")
public actual inline fun doubleArrayOf(vararg elements: Double): DoubleArray = elements

/**
 * Returns an array containing the specified [Float] numbers.
 *
 * @sample samples.collections.Arrays.Constructors.floatArrayOfSample
 */
@Suppress("NOTHING_TO_INLINE")
public actual inline fun floatArrayOf(vararg elements: Float): FloatArray = elements

/**
 * Returns an array containing the specified [Long] numbers.
 *
 * @sample samples.collections.Arrays.Constructors.longArrayOfSample
 */
@Suppress("NOTHING_TO_INLINE")
public actual inline fun longArrayOf(vararg elements: Long): LongArray = elements

/**
 * Returns an array containing the specified [Int] numbers.
 *
 * @sample samples.collections.Arrays.Constructors.intArrayOfSample
 */
@Suppress("NOTHING_TO_INLINE")
public actual inline fun intArrayOf(vararg elements: Int): IntArray = elements

/**
 * Returns an array containing the specified characters.
 *
 * @sample samples.collections.Arrays.Constructors.charArrayOfSample
 */
@Suppress("NOTHING_TO_INLINE")
public actual inline fun charArrayOf(vararg elements: Char): CharArray = elements

/**
 * Returns an array containing the specified [Short] numbers.
 *
 * @sample samples.collections.Arrays.Constructors.shortArrayOfSample
 */
@Suppress("NOTHING_TO_INLINE")
public actual inline fun shortArrayOf(vararg elements: Short): ShortArray = elements

/**
 * Returns an array containing the specified [Byte] numbers.
 *
 * @sample samples.collections.Arrays.Constructors.byteArrayOfSample
 */
@Suppress("NOTHING_TO_INLINE")
public actual inline fun byteArrayOf(vararg elements: Byte): ByteArray = elements

/**
 * Returns an array containing the specified boolean values.
 *
 * @sample samples.collections.Arrays.Constructors.booleanArrayOfSample
 */
@Suppress("NOTHING_TO_INLINE")
public actual inline fun booleanArrayOf(vararg elements: Boolean): BooleanArray = elements
