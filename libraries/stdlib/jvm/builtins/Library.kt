/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:kotlin.internal.JvmBuiltin
@file:kotlin.internal.SuppressBytecodeGeneration
@file:Suppress("NON_MEMBER_FUNCTION_NO_BODY", "REIFIED_TYPE_PARAMETER_NO_INLINE")

package kotlin

/**
 * Returns a string representation of the object. Can be called with a null receiver, in which case
 * it returns the string "null".
 */
public actual fun Any?.toString(): String

/**
 * Concatenates this string with the string representation of the given [other] object. If either the receiver
 * or the [other] object are null, they are represented as the string "null".
 */
public actual operator fun String?.plus(other: Any?): String

/**
 * Returns an array of objects of the given type with the given [size], initialized with null values.
 *
 * @throws RuntimeException if the specified [size] is negative.
 */
public actual fun <reified T> arrayOfNulls(size: Int): Array<T?>

/**
 * Returns an array containing the specified elements.
 */
public actual inline fun <reified T> arrayOf(vararg elements: T): Array<T>

/**
 * Returns an array containing the specified [Double] numbers.
 */
public actual fun doubleArrayOf(vararg elements: Double): DoubleArray

/**
 * Returns an array containing the specified [Float] numbers.
 */
public actual fun floatArrayOf(vararg elements: Float): FloatArray

/**
 * Returns an array containing the specified [Long] numbers.
 */
public actual fun longArrayOf(vararg elements: Long): LongArray

/**
 * Returns an array containing the specified [Int] numbers.
 */
public actual fun intArrayOf(vararg elements: Int): IntArray

/**
 * Returns an array containing the specified characters.
 */
public actual fun charArrayOf(vararg elements: Char): CharArray

/**
 * Returns an array containing the specified [Short] numbers.
 */
public actual fun shortArrayOf(vararg elements: Short): ShortArray

/**
 * Returns an array containing the specified [Byte] numbers.
 */
public actual fun byteArrayOf(vararg elements: Byte): ByteArray

/**
 * Returns an array containing the specified boolean values.
 */
public actual fun booleanArrayOf(vararg elements: Boolean): BooleanArray

/**
 * Returns an array containing enum T entries.
 */
@SinceKotlin("1.1")
public actual inline fun <reified T : Enum<T>> enumValues(): Array<T>

/**
 * Returns an enum entry with specified name.
 */
@SinceKotlin("1.1")
public actual inline fun <reified T : Enum<T>> enumValueOf(name: String): T