/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:kotlin.jvm.JvmMultifileClass
@file:kotlin.jvm.JvmName("StringsKt")
@file:Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")

package kotlin.text

/**
 * Returns a string representation of this [Byte] value in the specified [radix].
 *
 * @throws IllegalArgumentException when [radix] is not a valid radix for number to string conversion.
 */
@SinceKotlin("1.1")
@kotlin.internal.InlineOnly
public actual inline fun Byte.toString(radix: Int): String = this.toInt().toString(radix)

/**
 * Returns a string representation of this [Short] value in the specified [radix].
 *
 * @throws IllegalArgumentException when [radix] is not a valid radix for number to string conversion.
 */
@SinceKotlin("1.1")
@kotlin.internal.InlineOnly
public actual inline fun Short.toString(radix: Int): String = this.toInt().toString(radix)

/**
 * Returns a string representation of this [Int] value in the specified [radix].
 *
 * @throws IllegalArgumentException when [radix] is not a valid radix for number to string conversion.
 */
@SinceKotlin("1.1")
@kotlin.internal.InlineOnly
public actual inline fun Int.toString(radix: Int): String = java.lang.Integer.toString(this, checkRadix(radix))

/**
 * Returns a string representation of this [Long] value in the specified [radix].
 *
 * @throws IllegalArgumentException when [radix] is not a valid radix for number to string conversion.
 */
@SinceKotlin("1.1")
@kotlin.internal.InlineOnly
public actual inline fun Long.toString(radix: Int): String = java.lang.Long.toString(this, checkRadix(radix))

/**
 * Returns `true` if this string is not `null` and its content is equal to the word "true", ignoring case, and `false` otherwise.
 *
 * There are also strict versions of the function available on non-nullable String, [toBooleanStrict] and [toBooleanStrictOrNull].
 */
@SinceKotlin("1.4")
@kotlin.internal.InlineOnly
public actual inline fun String?.toBoolean(): Boolean = java.lang.Boolean.parseBoolean(this)

/**
 * Parses the string as a signed [Byte] number and returns the result.
 * @throws NumberFormatException if the string is not a valid representation of a number.
 */
@kotlin.internal.InlineOnly
public actual inline fun String.toByte(): Byte = java.lang.Byte.parseByte(this)

/**
 * Parses the string as a signed [Byte] number and returns the result.
 * @throws NumberFormatException if the string is not a valid representation of a number.
 * @throws IllegalArgumentException when [radix] is not a valid radix for string to number conversion.
 */
@SinceKotlin("1.1")
@kotlin.internal.InlineOnly
public actual inline fun String.toByte(radix: Int): Byte = java.lang.Byte.parseByte(this, checkRadix(radix))


/**
 * Parses the string as a [Short] number and returns the result.
 * @throws NumberFormatException if the string is not a valid representation of a number.
 */
@kotlin.internal.InlineOnly
public actual inline fun String.toShort(): Short = java.lang.Short.parseShort(this)

/**
 * Parses the string as a [Short] number and returns the result.
 * @throws NumberFormatException if the string is not a valid representation of a number.
 * @throws IllegalArgumentException when [radix] is not a valid radix for string to number conversion.
 */
@SinceKotlin("1.1")
@kotlin.internal.InlineOnly
public actual inline fun String.toShort(radix: Int): Short = java.lang.Short.parseShort(this, checkRadix(radix))

/**
 * Parses the string as an [Int] number and returns the result.
 * @throws NumberFormatException if the string is not a valid representation of a number.
 */
@kotlin.internal.InlineOnly
public actual inline fun String.toInt(): Int = java.lang.Integer.parseInt(this)

/**
 * Parses the string as an [Int] number and returns the result.
 * @throws NumberFormatException if the string is not a valid representation of a number.
 * @throws IllegalArgumentException when [radix] is not a valid radix for string to number conversion.
 */
@SinceKotlin("1.1")
@kotlin.internal.InlineOnly
public actual inline fun String.toInt(radix: Int): Int = java.lang.Integer.parseInt(this, checkRadix(radix))

/**
 * Parses the string as a [Long] number and returns the result.
 * @throws NumberFormatException if the string is not a valid representation of a number.
 */
@kotlin.internal.InlineOnly
public actual inline fun String.toLong(): Long = java.lang.Long.parseLong(this)

/**
 * Parses the string as a [Long] number and returns the result.
 * @throws NumberFormatException if the string is not a valid representation of a number.
 * @throws IllegalArgumentException when [radix] is not a valid radix for string to number conversion.
 */
@SinceKotlin("1.1")
@kotlin.internal.InlineOnly
public actual inline fun String.toLong(radix: Int): Long = java.lang.Long.parseLong(this, checkRadix(radix))

/**
 * Parses the string as a [Float] number and returns the result.
 * @throws NumberFormatException if the string is not a valid representation of a number.
 */
@kotlin.internal.InlineOnly
public actual inline fun String.toFloat(): Float = java.lang.Float.parseFloat(this)

/**
 * Parses the string as a [Double] number and returns the result.
 * @throws NumberFormatException if the string is not a valid representation of a number.
 */
@kotlin.internal.InlineOnly
public actual inline fun String.toDouble(): Double = java.lang.Double.parseDouble(this)


/**
 * Parses the string as a [Float] number and returns the result
 * or `null` if the string is not a valid representation of a number.
 */
@SinceKotlin("1.1")
public actual fun String.toFloatOrNull(): Float? = screenFloatValue(this, java.lang.Float::parseFloat)

/**
 * Parses the string as a [Double] number and returns the result
 * or `null` if the string is not a valid representation of a number.
 */
@SinceKotlin("1.1")
public actual fun String.toDoubleOrNull(): Double? = screenFloatValue(this, java.lang.Double::parseDouble)

/**
 * Parses the string as a [java.math.BigInteger] number and returns the result.
 * @throws NumberFormatException if the string is not a valid representation of a number.
 */
@SinceKotlin("1.2")
@kotlin.internal.InlineOnly
public inline fun String.toBigInteger(): java.math.BigInteger =
    java.math.BigInteger(this)

/**
 * Parses the string as a [java.math.BigInteger] number and returns the result.
 * @throws NumberFormatException if the string is not a valid representation of a number.
 * @throws IllegalArgumentException when [radix] is not a valid radix for string to number conversion.
 */
@SinceKotlin("1.2")
@kotlin.internal.InlineOnly
public inline fun String.toBigInteger(radix: Int): java.math.BigInteger =
    java.math.BigInteger(this, checkRadix(radix))

/**
 * Parses the string as a [java.math.BigInteger] number and returns the result
 * or `null` if the string is not a valid representation of a number.
 */
@SinceKotlin("1.2")
public fun String.toBigIntegerOrNull(): java.math.BigInteger? = toBigIntegerOrNull(10)

/**
 * Parses the string as a [java.math.BigInteger] number and returns the result
 * or `null` if the string is not a valid representation of a number.
 *
 * @throws IllegalArgumentException when [radix] is not a valid radix for string to number conversion.
 */
@SinceKotlin("1.2")
public fun String.toBigIntegerOrNull(radix: Int): java.math.BigInteger? {
    checkRadix(radix)
    val length = this.length
    when (length) {
        0 -> return null
        1 -> if (digitOf(this[0], radix) < 0) return null
        else -> {
            val start = if (this[0] == '-') 1 else 0
            for (index in start until length) {
                if (digitOf(this[index], radix) < 0)
                    return null
            }
        }
    }
    return toBigInteger(radix)
}


/**
 * Parses the string as a [java.math.BigDecimal] number and returns the result.
 * @throws NumberFormatException if the string is not a valid representation of a number.
 */
@SinceKotlin("1.2")
@kotlin.internal.InlineOnly
public inline fun String.toBigDecimal(): java.math.BigDecimal =
    java.math.BigDecimal(this)

/**
 * Parses the string as a [java.math.BigDecimal] number and returns the result.
 * @throws NumberFormatException if the string is not a valid representation of a number.
 *
 * @param mathContext specifies the precision and the rounding mode.
 * @throws ArithmeticException if the rounding is needed, but the rounding mode is [java.math.RoundingMode.UNNECESSARY].
 */
@SinceKotlin("1.2")
@kotlin.internal.InlineOnly
public inline fun String.toBigDecimal(mathContext: java.math.MathContext): java.math.BigDecimal =
    java.math.BigDecimal(this, mathContext)

/**
 * Parses the string as a [java.math.BigDecimal] number and returns the result
 * or `null` if the string is not a valid representation of a number.
 */
@SinceKotlin("1.2")
public fun String.toBigDecimalOrNull(): java.math.BigDecimal? =
    screenFloatValue(this) { it.toBigDecimal() }

/**
 * Parses the string as a [java.math.BigDecimal] number and returns the result
 * or `null` if the string is not a valid representation of a number.
 *
 * @param mathContext specifies the precision and the rounding mode.
 * @throws ArithmeticException if the rounding is needed, but the rounding mode is [java.math.RoundingMode.UNNECESSARY].
 */
@SinceKotlin("1.2")
public fun String.toBigDecimalOrNull(mathContext: java.math.MathContext): java.math.BigDecimal? =
    screenFloatValue(this) { it.toBigDecimal(mathContext) }

private inline fun <T> screenFloatValue(str: String, parse: (String) -> T): T? {
    return try {
        if (isValidFloat(str))
            parse(str)
        else
            null
    } catch (_: NumberFormatException) {  // overflow
        null
    }
}

private const val LengthOfNaN = 2 // "NaN".length - 1
private const val LengthOfInfinity = 7 // "Infinity".length - 1

fun isFloat(s: String): Boolean {
    // A float can have one of two representations:
    //
    // 1. Standard:
    //     - With an integer part only: 1234
    //     - With integer and fractional parts: 1234.4678
    //     - With a fractional part only: .4678
    //
    //     Optional sign prefix: + or -
    //     Optional signed exponent: e or E, followed by optionally signed digits (+12, -12, 12)
    //     Optional suffix: f, F, d, or D (for instance 12.34f or .34D)
    //
    // 2. Hexadecimal:
    //     - With an integer part only: 0x12ab
    //     - With integer and fractional parts: 0x12ab.CD78
    //     - With a fractional part only: 0x.CD78
    //
    //     Mandatory signed exponent: p or P, followed by optionally signed digits (+12, -12, 12)
    //
    //     Optional sign prefix: + or -
    //     Optional suffix: f, F, d, or D (for instance 12.34f or .34D)
    //
    // Two special cases:
    //     "NaN" and "Infinity" strings, can have an optional sign prefix (+ or -)
    //
    // Implementation notes:
    //     - The pattern "myChar.code or 0x20 == 'x'.code" is used to perform a case-insensitive
    //       comparison of a character. Adding the 0x20 bit turns an upper case ASCII letter into
    //       a lower case one.

    var start = 0
    var end = s.length - 1

    // Skip leading spaces
    while (start <= end && s[start].code <= 0x20) start++

    // Empty/whitespace string
    if (start > end) return false

    // Skip trailing spaces
    while (end > start && s[end].code <= 0x20) end--

    // Number starts with a positive or negative sign
    if (s[start] == '+' || s[start] == '-') start++
    // If we have nothing after the sign, the string is invalid
    if (start > end) return false

    var hasIntegerPart: Boolean
    var hasFractionalPart = false
    var isHex = false

    // Might be a hex string
    if (s[start] == '0') {
        start++
        // A "0" on its own is valid
        if (start > end) return true

        // Test for [xX] to see if we truly have a hex string
        if (s[start].code or 0x20 == 'x'.code) {
            start++

            // Look for hex digits after the 0x prefix
            var checkpoint = start
            while (start <= end) {
                val d = s[start].code
                val l = d or 0x20
                if ((d >= '0'.code && d <= '9'.code) || (l >= 'a'.code && l <= 'f'.code)) {
                    start++
                } else {
                    break
                }
            }
            // Check if we found 0x*****, otherwise, the hex number might be of the
            // form 0x.*******
            hasIntegerPart = checkpoint != start

            // A hex string must have an exponent, the string is invalid if we only found an
            // integer part
            if (start > end) return false

            if (s[start] == '.') {
                start++

                // Look for hex digits for the fractional part
                checkpoint = start
                while (start <= end) {
                    val d = s[start].code // used to test digits
                    val l = d or 0x20 // used to test letters A-F
                    if ((d >= '0'.code && d <= '9'.code) || (l >= 'a'.code && l <= 'f'.code)) {
                        start++
                    } else {
                        break
                    }
                }

                // Did we find a fractional part?
                hasFractionalPart = checkpoint != start
            }

            // A string must have an integer part, or a fractional part, or both
            if (!hasIntegerPart && !hasFractionalPart) return false

            // A hex string must have an exponent, the string is invalid if we only found an
            // integer and/or fractional part
            if (start > end) return false

            isHex = true
        } else {
            // Rewind the 0 we just parsed to make things easier below and try to parse a non-
            // hexadecimal string representation of a float
            start--
        }
    }

    // Parse a non-hexadecimal representations
    if (!isHex) {
        // Look for digits before the decimal separator, if any
        var checkpoint = start
        while (start <= end) {
            val d = s[start].code
            if (d >= '0'.code && d <= '9'.code) {
                start++
            } else {
                break
            }
        }

        // If there's no integer part, the float might be of the form .1234
        hasIntegerPart = checkpoint != start

        // A non-hexadecimal representation only needs an integer part, we can stop here
        if (start > end) return hasIntegerPart

        if (s[start] == '.') {
            start++

            // Look for the fractional part
            checkpoint = start
            while (start <= end) {
                val d = s[start].code
                if (d >= '0'.code && d <= '9'.code) {
                    start++
                } else {
                    break
                }
            }

            // Did we find a fractional part?
            hasFractionalPart = checkpoint != start
        }

        // A string must have an integer part, or a fractional part, or both
        if (!hasIntegerPart && !hasFractionalPart) {
            // Special case non-finite constants
            val constant = when (end) {
                start + LengthOfNaN -> {
                    "NaN"
                }
                start + LengthOfInfinity -> {
                    "Infinity"
                }
                else -> {
                    // If we don't have enough characters left for the 2 known constants, just bail
                    return false
                }
            }
            return s.indexOf(constant, start, false) == start
        }

        // If we have either, we can stop here if we've run out of characters
        if (start > end) return true
    }

    // Look for an exponent:
    //     - Mandatory for hexadecimal strings (marked by a p or P)
    //     - Optional for "regular" strings (marked by an e or E)
    var l = s[start++].code or 0x20
    if (l != if (isHex) 'p'.code else 'e'.code) {
        // We're here if the exponent character is not valid, but if the string is a "regular"
        // string, it could be a valid f/F/d/D suffix, so check for that (it must be the last
        // character too)
        return !isHex && (l == 'f'.code || l == 'd'.code) && start > end
    }

    // An exponent must be followed by digits
    if (start > end) return false

    // There may be a sign prefix before the exponent digits
    if (s[start] == '+' || s[start] == '-') {
        start++
        if (start > end) return false
    }

    // Look for digits after the exponent and its optional sign
    while (start <= end) {
        val d = s[start]
        if (d in '0'..'9') {
            start++
        } else {
            break
        }
    }

    // The last suffix is optional, the string is valid here
    if (start > end) return true

    // We may have an optional fFdD suffix
    if (start == end) {
        l = s[start].code or 0x20
        return l == 'f'.code || l == 'd'.code
    }

    // Anything left is invalid
    return false
}


