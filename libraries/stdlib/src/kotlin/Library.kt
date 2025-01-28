/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin

import kotlin.internal.ActualizeByJvmBuiltinProvider

/**
 * Returns a string representation of the object. Can be called with a null receiver, in which case
 * it returns the string "null".
 */
@ActualizeByJvmBuiltinProvider
public expect fun Any?.toString(): String

/**
 * Concatenates this string with the string representation of the given [other] object. If either the receiver
 * or the [other] object are null, they are represented as the string "null".
 */
@ActualizeByJvmBuiltinProvider
public expect operator fun String?.plus(other: Any?): String

/**
 * Returns an array of objects of the given type with the given [size], initialized with null values.
 *
 * @throws RuntimeException if the specified [size] is negative.
 */
@Suppress("REIFIED_TYPE_PARAMETER_NO_INLINE")
@ActualizeByJvmBuiltinProvider
public expect fun <reified T> arrayOfNulls(size: Int): Array<T?>

/**
 * Returns an array containing the specified elements.
 */
@ActualizeByJvmBuiltinProvider
public expect inline fun <reified T> arrayOf(vararg elements: T): Array<T>

/**
 * Returns an array containing the specified [Double] numbers.
 */
@ActualizeByJvmBuiltinProvider
public expect fun doubleArrayOf(vararg elements: Double): DoubleArray

/**
 * Returns an array containing the specified [Float] numbers.
 */
@ActualizeByJvmBuiltinProvider
public expect fun floatArrayOf(vararg elements: Float): FloatArray

/**
 * Returns an array containing the specified [Long] numbers.
 */
@ActualizeByJvmBuiltinProvider
public expect fun longArrayOf(vararg elements: Long): LongArray

/**
 * Returns an array containing the specified [Int] numbers.
 */
@ActualizeByJvmBuiltinProvider
public expect fun intArrayOf(vararg elements: Int): IntArray

/**
 * Returns an array containing the specified characters.
 */
@ActualizeByJvmBuiltinProvider
public expect fun charArrayOf(vararg elements: Char): CharArray

/**
 * Returns an array containing the specified [Short] numbers.
 */
@ActualizeByJvmBuiltinProvider
public expect fun shortArrayOf(vararg elements: Short): ShortArray

/**
 * Returns an array containing the specified [Byte] numbers.
 */
@ActualizeByJvmBuiltinProvider
public expect fun byteArrayOf(vararg elements: Byte): ByteArray

/**
 * Returns an array containing the specified boolean values.
 */
@ActualizeByJvmBuiltinProvider
public expect fun booleanArrayOf(vararg elements: Boolean): BooleanArray

/**
 * Returns an array containing enum T entries.
 */
@SinceKotlin("1.1")
@ActualizeByJvmBuiltinProvider
public expect inline fun <reified T : Enum<T>> enumValues(): Array<T>

/**
 * Returns an enum entry with specified name.
 */
@SinceKotlin("1.1")
@ActualizeByJvmBuiltinProvider
public expect inline fun <reified T : Enum<T>> enumValueOf(name: String): T
