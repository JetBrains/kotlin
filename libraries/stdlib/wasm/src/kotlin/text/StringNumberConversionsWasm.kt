/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.text

import kotlin.wasm.internal.wasm_f32_demote_f64

/**
 * Returns `true` if this string is not `null` and its content is equal to the word "true", ignoring case, and `false` otherwise.
 *
 * There are also strict versions of the function available on non-nullable String, [toBooleanStrict] and [toBooleanStrictOrNull].
 */
public actual fun String?.toBoolean(): Boolean = this != null && this.lowercase() == "true"

/**
 * Parses the string to a [Byte] number.
 *
 * The string must consist of an optional leading `+` or `-` sign and decimal digits (`0-9`),
 * and fit the valid [Byte] value range (within `Byte.MIN_VALUE..Byte.MAX_VALUE`),
 * otherwise a [NumberFormatException] will be thrown.
 *
 * @throws NumberFormatException if the string is not a valid representation of a [Byte].
 * @sample samples.text.Numbers.toByte
 */
public actual fun String.toByte(): Byte = toByteOrNull() ?: numberFormatError(this)

/**
 * Parses the string as a signed [Byte] number and returns the result.
 * @throws NumberFormatException if the string is not a valid representation of a number.
 * @throws IllegalArgumentException when [radix] is not a valid radix for string to number conversion.
 */
public actual fun String.toByte(radix: Int): Byte = toByteOrNull(radix) ?: numberFormatError(this)

/**
 * Parses the string to a [Short] number.
 *
 * The string must consist of an optional leading `+` or `-` sign and decimal digits (`0-9`),
 * and fit the valid [Short] value range (within `Short.MIN_VALUE..Short.MAX_VALUE`),
 * otherwise a [NumberFormatException] will be thrown.
 *
 * @throws NumberFormatException if the string is not a valid representation of a [Short].
 * @sample samples.text.Numbers.toShort
 */
public actual fun String.toShort(): Short = toShortOrNull() ?: numberFormatError(this)

/**
 * Parses the string as a [Short] number and returns the result.
 * @throws NumberFormatException if the string is not a valid representation of a number.
 * @throws IllegalArgumentException when [radix] is not a valid radix for string to number conversion.
 */
public actual fun String.toShort(radix: Int): Short = toShortOrNull(radix) ?: numberFormatError(this)

/**
 * Parses the string to an [Int] number.
 *
 * The string must consist of an optional leading `+` or `-` sign and decimal digits (`0-9`),
 * and fit the valid [Int] value range (within `Int.MIN_VALUE..Int.MAX_VALUE`),
 * otherwise a [NumberFormatException] will be thrown.
 *
 * @throws NumberFormatException if the string is not a valid representation of an [Int].
 * @sample samples.text.Numbers.toInt
 */
public actual fun String.toInt(): Int = toIntOrNull() ?: numberFormatError(this)

/**
 * Parses the string as an [Int] number and returns the result.
 * @throws NumberFormatException if the string is not a valid representation of a number.
 * @throws IllegalArgumentException when [radix] is not a valid radix for string to number conversion.
 */
public actual fun String.toInt(radix: Int): Int = toIntOrNull(radix) ?: numberFormatError(this)

/**
 * Parses the string to a [Long] number.
 *
 * The string must consist of an optional leading `+` or `-` sign and decimal digits (`0-9`),
 * and fit the valid [Long] value range (within `Long.MIN_VALUE..Long.MAX_VALUE`),
 * otherwise a [NumberFormatException] will be thrown.
 *
 * @throws NumberFormatException if the string is not a valid representation of a [Long].
 * @sample samples.text.Numbers.toLong
 */
public actual fun String.toLong(): Long = toLongOrNull() ?: numberFormatError(this)

/**
 * Parses the string as a [Long] number and returns the result.
 * @throws NumberFormatException if the string is not a valid representation of a number.
 * @throws IllegalArgumentException when [radix] is not a valid radix for string to number conversion.
 */
public actual fun String.toLong(radix: Int): Long = toLongOrNull(radix) ?: numberFormatError(this)

/**
 * Parses the string as a [Double] number and returns the result.
 * @throws NumberFormatException if the string is not a valid representation of a number.
 */
public actual fun String.toDouble(): Double = kotlin.text.parseDouble(this)

/**
 * Parses the string as a [Float] number and returns the result.
 * @throws NumberFormatException if the string is not a valid representation of a number.
 */
public actual fun String.toFloat(): Float = wasm_f32_demote_f64(toDouble())

/**
 * Parses the string as a [Float] number and returns the result
 * or `null` if the string is not a valid representation of a number.
 */
public actual fun String.toFloatOrNull(): Float? = toDoubleOrNull()?.let { wasm_f32_demote_f64(it) }

/**
 * Parses the string as a [Double] number and returns the result
 * or `null` if the string is not a valid representation of a number.
 */
public actual fun String.toDoubleOrNull(): Double? {
    try {
        return toDouble()
    } catch (e: NumberFormatException) {
        return null
    }
}

/**
 * Returns a string representation of this [Byte] value in the specified [radix].
 *
 * @throws IllegalArgumentException when [radix] is not a valid radix for number to string conversion.
 */
@SinceKotlin("1.2")
public actual fun Byte.toString(radix: Int): String = this.toInt().toString(radix)

/**
 * Returns a string representation of this [Short] value in the specified [radix].
 *
 * @throws IllegalArgumentException when [radix] is not a valid radix for number to string conversion.
 */
@SinceKotlin("1.2")
public actual fun Short.toString(radix: Int): String = this.toInt().toString(radix)

/**
 * Returns a string representation of this [Int] value in the specified [radix].
 *
 * @throws IllegalArgumentException when [radix] is not a valid radix for number to string conversion.
 */
@SinceKotlin("1.2")
public actual fun Int.toString(radix: Int): String {
    val isNegative = this < 0
    val absValue = if (isNegative) -this else this
    val absValueString = uintToString(absValue, checkRadix(radix))

    return if (isNegative) "-$absValueString" else absValueString
}

/**
 * Returns a string representation of this [Long] value in the specified [radix].
 *
 * @throws IllegalArgumentException when [radix] is not a valid radix for number to string conversion.
 */
@SinceKotlin("1.2")
public actual fun Long.toString(radix: Int): String {
    val isNegative = this < 0
    val absValue = if (isNegative) -this else this
    val absValueString = ulongToString(absValue, checkRadix(radix))

    return if (isNegative) "-$absValueString" else absValueString
}
