/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("NOTHING_TO_INLINE")

package kotlin

import kotlin.wasm.internal.enumValueOfIntrinsic
import kotlin.wasm.internal.enumValuesIntrinsic

public actual inline fun <T> emptyArray(): Array<T> = arrayOf()

/**
 * Returns a string representation of the object. Can be called with a null receiver, in which case
 * it returns the string "null".
 */
public actual fun Any?.toString(): String = this?.toString() ?: "null"

/**
 * Concatenates this string with the string representation of the given [other] object. If either the receiver
 * or the [other] object are null, they are represented as the string "null".
 */
public actual operator fun String?.plus(other: Any?): String = (this ?: "null") + other.toString()

/**
 * Returns an array of objects of the given type with the given [size], initialized with null values.
 *
 * @throws RuntimeException if the specified [size] is negative.
 */
@Suppress("UNCHECKED_CAST")
public actual inline fun <T> arrayOfNulls(size: Int): Array<T?> = Array<Any?>(size) as Array<T?>

/**
 * Returns an array containing the specified elements.
 *
 * @sample samples.collections.Arrays.Constructors.arrayOfSample
 */
@Suppress("UNCHECKED_CAST")
public actual inline fun <T> arrayOf(vararg elements: T): Array<T> = elements as Array<T>

/**
 * Returns an array containing the specified [Double] numbers.
 *
 * @sample samples.collections.Arrays.Constructors.doubleArrayOfSample
 */
public actual inline fun doubleArrayOf(vararg elements: Double): DoubleArray = elements

/**
 * Returns an array containing the specified [Float] numbers.
 *
 * @sample samples.collections.Arrays.Constructors.floatArrayOfSample
 */
public actual inline fun floatArrayOf(vararg elements: Float): FloatArray = elements

/**
 * Returns an array containing the specified [Long] numbers.
 *
 * @sample samples.collections.Arrays.Constructors.longArrayOfSample
 */
public actual inline fun longArrayOf(vararg elements: Long): LongArray = elements

/**
 * Returns an array containing the specified [Int] numbers.
 *
 * @sample samples.collections.Arrays.Constructors.intArrayOfSample
 */
public actual inline fun intArrayOf(vararg elements: Int): IntArray = elements

/**
 * Returns an array containing the specified characters.
 *
 * @sample samples.collections.Arrays.Constructors.charArrayOfSample
 */
public actual inline fun charArrayOf(vararg elements: Char): CharArray = elements

/**
 * Returns an array containing the specified [Short] numbers.
 *
 * @sample samples.collections.Arrays.Constructors.shortArrayOfSample
 */
public actual inline fun shortArrayOf(vararg elements: Short): ShortArray = elements

/**
 * Returns an array containing the specified [Byte] numbers.
 *
 * @sample samples.collections.Arrays.Constructors.byteArrayOfSample
 */
public actual inline fun byteArrayOf(vararg elements: Byte): ByteArray = elements

/**
 * Returns an array containing the specified boolean values.
 *
 * @sample samples.collections.Arrays.Constructors.booleanArrayOfSample
 */
public actual inline fun booleanArrayOf(vararg elements: Boolean): BooleanArray = elements

/**
 * Returns an array containing enum entries of the enum type [T].
 *
 * The function returns a new instance of the array on every call.
 * The array could be mutated, so working with it may also require defensive copying.
 * Consider using [kotlin.enums.enumEntries] as a more efficient alternative
 * returning an immutable list of enum entries.
 *
 * @see kotlin.enums.enumEntries
 */
@SinceKotlin("1.1")
public actual inline fun <reified T : Enum<T>> enumValues(): Array<T> =
    enumValuesIntrinsic()

/**
 * Returns the enum entry of type [T] with the specified [name].
 *
 * The [name] must exactly match an existing enum constant of type [T] (case-sensitive).
 *
 * @throws IllegalArgumentException if no enum constant with the specified [name] exists in [T].
 * @sample samples.misc.Enums.enumValueOfSample
 */
@SinceKotlin("1.1")
public actual inline fun <reified T : Enum<T>> enumValueOf(name: String): T =
    enumValueOfIntrinsic(name)
