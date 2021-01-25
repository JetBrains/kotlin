/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package kotlin.text

import kotlin.native.internal.FloatingPointParser
import kotlin.native.internal.GCCritical

/**
 * Returns a string representation of this [Byte] value in the specified [radix].
 *
 * @throws IllegalArgumentException when [radix] is not a valid radix for number to string conversion.
 */
@SinceKotlin("1.1")
@kotlin.internal.InlineOnly
public actual inline fun Byte.toString(radix: Int): String = this.toInt().toString(checkRadix(radix))

/**
 * Returns a string representation of this [Short] value in the specified [radix].
 *
 * @throws IllegalArgumentException when [radix] is not a valid radix for number to string conversion.
 */
@SinceKotlin("1.1")
@kotlin.internal.InlineOnly
public actual inline fun Short.toString(radix: Int): String = this.toInt().toString(checkRadix(radix))

@SymbolName("Kotlin_Int_toStringRadix")
@GCCritical
@PublishedApi
external internal fun intToString(value: Int, radix: Int): String

/**
 * Returns a string representation of this [Int] value in the specified [radix].
 *
 * @throws IllegalArgumentException when [radix] is not a valid radix for number to string conversion.
 */
@SinceKotlin("1.1")
@kotlin.internal.InlineOnly
public actual inline fun Int.toString(radix: Int): String = intToString(this, checkRadix(radix))

@SymbolName("Kotlin_Long_toStringRadix")
@GCCritical
@PublishedApi
external internal fun longToString(value: Long, radix: Int): String

/**
 * Returns a string representation of this [Long] value in the specified [radix].
 *
 * @throws IllegalArgumentException when [radix] is not a valid radix for number to string conversion.
 */
@SinceKotlin("1.1")
@kotlin.internal.InlineOnly
public actual inline fun Long.toString(radix: Int): String = longToString(this, checkRadix(radix))

/**
 * Returns `true` if the content of this string is equal to the word "true", ignoring case, and `false` otherwise.
 */
@Deprecated("Use Kotlin compiler 1.4 to avoid deprecation warning.")
@DeprecatedSinceKotlin(hiddenSince = "1.4")
@kotlin.internal.InlineOnly
public actual inline fun String.toBoolean(): Boolean = this.toBoolean()

/**
 * Returns `true` if this string is not `null` and its content is equal to the word "true", ignoring case, and `false` otherwise.
 */
@SinceKotlin("1.4")
@kotlin.internal.InlineOnly
public actual inline fun String?.toBoolean(): Boolean = this.equals("true", ignoreCase = true)

/**
 * Parses the string as a signed [Byte] number and returns the result.
 * @throws NumberFormatException if the string is not a valid representation of a number.
 */
@kotlin.internal.InlineOnly
public actual inline fun String.toByte(): Byte = toByteOrNull() ?: throw NumberFormatException()

/**
 * Parses the string as a signed [Byte] number and returns the result.
 * @throws NumberFormatException if the string is not a valid representation of a number.
 * @throws IllegalArgumentException when [radix] is not a valid radix for string to number conversion.
 */
@SinceKotlin("1.1")
@kotlin.internal.InlineOnly
public actual inline fun String.toByte(radix: Int): Byte = toByteOrNull(radix) ?: throw NumberFormatException()

/**
 * Parses the string as a [Short] number and returns the result.
 * @throws NumberFormatException if the string is not a valid representation of a number.
 */
@kotlin.internal.InlineOnly
public actual inline fun String.toShort(): Short = toShortOrNull() ?: throw NumberFormatException()

/**
 * Parses the string as a [Short] number and returns the result.
 * @throws NumberFormatException if the string is not a valid representation of a number.
 * @throws IllegalArgumentException when [radix] is not a valid radix for string to number conversion.
 */
@SinceKotlin("1.1")
@kotlin.internal.InlineOnly
public actual inline fun String.toShort(radix: Int): Short = toShortOrNull(radix) ?: throw NumberFormatException()

/**
 * Parses the string as an [Int] number and returns the result.
 * @throws NumberFormatException if the string is not a valid representation of a number.
 */
@kotlin.internal.InlineOnly
public actual inline fun String.toInt(): Int = toIntOrNull() ?: throw NumberFormatException()

/**
 * Parses the string as an [Int] number and returns the result.
 * @throws NumberFormatException if the string is not a valid representation of a number.
 * @throws IllegalArgumentException when [radix] is not a valid radix for string to number conversion.
 */
@SinceKotlin("1.1")
@kotlin.internal.InlineOnly
public actual inline fun String.toInt(radix: Int): Int = toIntOrNull(radix) ?: throw NumberFormatException()

/**
 * Parses the string as a [Long] number and returns the result.
 * @throws NumberFormatException if the string is not a valid representation of a number.
 */
@kotlin.internal.InlineOnly
public actual inline fun String.toLong(): Long = toLongOrNull() ?: throw NumberFormatException()

/**
 * Parses the string as a [Long] number and returns the result.
 * @throws NumberFormatException if the string is not a valid representation of a number.
 * @throws IllegalArgumentException when [radix] is not a valid radix for string to number conversion.
 */
@SinceKotlin("1.1")
@kotlin.internal.InlineOnly
public actual inline fun String.toLong(radix: Int): Long = toLongOrNull(radix) ?: throw NumberFormatException()

/**
 * Parses the string as a [Float] number and returns the result.
 * @throws NumberFormatException if the string is not a valid representation of a number.
 */
@kotlin.internal.InlineOnly
public actual inline fun String.toFloat(): Float = FloatingPointParser.parseFloat(this)


/**
 * Parses the string as a [Double] number and returns the result.
 * @throws NumberFormatException if the string is not a valid representation of a number.
 */
@kotlin.internal.InlineOnly
public actual inline fun String.toDouble(): Double = FloatingPointParser.parseDouble(this)

/**
 * Parses the string as a [Float] number and returns the result
 * or `null` if the string is not a valid representation of a number.
 */
@SinceKotlin("1.1")
public actual fun String.toFloatOrNull(): Float? {
    try {
        return toFloat()
    } catch (e: NumberFormatException) {
        return null
    }
}

/**
 * Parses the string as a [Double] number and returns the result
 * or `null` if the string is not a valid representation of a number.
 */
@SinceKotlin("1.1")
public actual fun String.toDoubleOrNull(): Double? {
    try {
        return toDouble()
    } catch (e: NumberFormatException) {
        return null
    }
}
