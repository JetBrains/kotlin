/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin


/**
 * Returns a string representation of the object. Can be called with a null receiver, in which case
 * it returns the string "null".
 */
public expect fun Any?.toString(): String

/**
 * Concatenates this string with the string representation of the given [other] object. If either the receiver
 * or the [other] object are null, they are represented as the string "null".
 */
public expect operator fun String?.plus(other: Any?): String

/**
 * Returns an array of objects of the given type with the given [size], initialized with null values.
 *
 * @throws RuntimeException if the specified [size] is negative.
 */
@Suppress("REIFIED_TYPE_PARAMETER_NO_INLINE")
public expect fun <reified T> arrayOfNulls(size: Int): Array<T?>

/**
 * Returns an array containing the specified elements.
 *
 * @sample samples.collections.Arrays.Constructors.arrayOfSample
 */
public expect inline fun <reified T> arrayOf(vararg elements: T): Array<T>

/**
 * Returns an array containing the specified [Double] numbers.
 *
 * @sample samples.collections.Arrays.Constructors.doubleArrayOfSample
 */
public expect fun doubleArrayOf(vararg elements: Double): DoubleArray

/**
 * Returns an array containing the specified [Float] numbers.
 *
 * @sample samples.collections.Arrays.Constructors.floatArrayOfSample
 */
public expect fun floatArrayOf(vararg elements: Float): FloatArray

/**
 * Returns an array containing the specified [Long] numbers.
 *
 * @sample samples.collections.Arrays.Constructors.longArrayOfSample
 */
public expect fun longArrayOf(vararg elements: Long): LongArray

/**
 * Returns an array containing the specified [Int] numbers.
 *
 * @sample samples.collections.Arrays.Constructors.intArrayOfSample
 */
public expect fun intArrayOf(vararg elements: Int): IntArray

/**
 * Returns an array containing the specified characters.
 *
 * @sample samples.collections.Arrays.Constructors.charArrayOfSample
 */
public expect fun charArrayOf(vararg elements: Char): CharArray

/**
 * Returns an array containing the specified [Short] numbers.
 *
 * @sample samples.collections.Arrays.Constructors.shortArrayOfSample
 */
public expect fun shortArrayOf(vararg elements: Short): ShortArray

/**
 * Returns an array containing the specified [Byte] numbers.
 *
 * @sample samples.collections.Arrays.Constructors.byteArrayOfSample
 */
public expect fun byteArrayOf(vararg elements: Byte): ByteArray

/**
 * Returns an array containing the specified boolean values.
 *
 * @sample samples.collections.Arrays.Constructors.booleanArrayOfSample
 */
public expect fun booleanArrayOf(vararg elements: Boolean): BooleanArray

/**
 * Returns an array containing enum T entries.
 */
@SinceKotlin("1.1")
public expect inline fun <reified T : Enum<T>> enumValues(): Array<T>

/**
 * Returns an enum entry with specified name.
 */
@SinceKotlin("1.1")
public expect inline fun <reified T : Enum<T>> enumValueOf(name: String): T
