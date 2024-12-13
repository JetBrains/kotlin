/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
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
 * Parses the string to a [Byte] number.
 *
 * The string must consist of an optional leading `+` or `-` sign and decimal digits (`0-9`),
 * and fit the valid [Byte] value range (within `Byte.MIN_VALUE..Byte.MAX_VALUE`),
 * otherwise a [NumberFormatException] will be thrown.
 *
 * @throws NumberFormatException if the string is not a valid representation of a [Byte].
 * @sample samples.text.Numbers.toByte
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
 * Parses the string to a [Short] number.
 *
 * The string must consist of an optional leading `+` or `-` sign and decimal digits (`0-9`),
 * and fit the valid [Short] value range (within `Short.MIN_VALUE..Short.MAX_VALUE`),
 * otherwise a [NumberFormatException] will be thrown.
 *
 * @throws NumberFormatException if the string is not a valid representation of a [Short].
 * @sample samples.text.Numbers.toShort
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
 * Parses the string to an [Int] number.
 *
 * The string must consist of an optional leading `+` or `-` sign and decimal digits (`0-9`),
 * and fit the valid [Int] value range (within `Int.MIN_VALUE..Int.MAX_VALUE`),
 * otherwise a [NumberFormatException] will be thrown.
 *
 * @throws NumberFormatException if the string is not a valid representation of an [Int].
 * @sample samples.text.Numbers.toInt
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
 * Parses the string to a [Long] number.
 *
 * The string must consist of an optional leading `+` or `-` sign and decimal digits (`0-9`),
 * and fit the valid [Long] value range (within `Long.MIN_VALUE..Long.MAX_VALUE`),
 * otherwise a [NumberFormatException] will be thrown.
 *
 * @throws NumberFormatException if the string is not a valid representation of a [Long].
 * @sample samples.text.Numbers.toLong
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

private fun isValidFloat(s: String): Boolean {
    // A float can have one of two representations:
    //
    // 1. Standard:
    //     - With an integer part only: 1234
    //.    - With an integer part followed by the decimal point: 1234.
    //     - With integer and fractional parts: 1234.4678
    //     - With a fractional part only: .4678
    //
    //     Optional sign prefix: + or -
    //     Optional signed exponent: e or E, followed by optionally signed digits (+12, -12, 12)
    //     Optional suffix: f, F, d, or D (for instance 12.34f or .34D)
    //
    // 2. Hexadecimal:
    //     - With an integer part only: 0x12ab
    //     - With an integer part followed by the decimal point: 0x12ab.
    //     - With integer and fractional parts: 0x12ab.CD78
    //     - With a fractional part only: 0x.CD78
    //
    //     Mandatory signed exponent: p or P, followed by optionally signed decimal digits (+12, -12, 12)
    //
    //     Optional sign prefix: + or -
    //     Optional suffix: f, F, d, or D (for instance 0xAB.01P1f or 0x.34P0D)
    //
    // Two special cases:
    //     "NaN" and "Infinity" strings, can have an optional sign prefix (+ or -)
    //
    // Implementation notes:
    //     - The pattern "myChar.code or 0x20 == 'x'.code" is used to perform a case-insensitive
    //       comparison of a character. Adding the 0x20 bit turns an upper case ASCII letter into
    //       a lower case one. This is encapsulated in the asciiLetterToLowerCaseCode() extension

    var start = 0
    var endInclusive = s.length - 1

    // Skip leading spaces
    start = s.advanceWhile(start, endInclusive) { it.code <= 0x20 }

    // Empty/whitespace string
    if (start > endInclusive) return false

    // Skip trailing spaces
    endInclusive = s.backtrackWhile(start, endInclusive) { it.code <= 0x20 }

    // Number starts with a positive or negative sign
    if (s[start] == '+' || s[start] == '-') start++
    // If we have nothing after the sign, the string is invalid
    if (start > endInclusive) return false

    var isHex = false

    // Might be a hex string
    if (s[start] == '0') {
        start++
        // A "0" on its own is valid
        if (start > endInclusive) return true

        // Test for [xX] to see if we truly have a hex string
        if (s[start].asciiLetterToLowerCaseCode() == 'x'.code) {
            start++

            start = s.advanceAndValidateMantissa(start, endInclusive, true) { it.isAsciiDigit() || it.isHexLetter() }

            // A hex string must have an exponent, the string is invalid if we only found an
            // integer and/or fractional part
            if (start == -1 || start > endInclusive) return false

            isHex = true
        } else {
            // Rewind the 0 we just parsed to make things easier below and try to parse a non-
            // hexadecimal string representation of a float
            start--
        }
    }

    // Parse a non-hexadecimal representations
    if (!isHex) {
        start = s.advanceAndValidateMantissa(start, endInclusive, false) { it.isAsciiDigit() }

        // We couldn't validate the mantissa, stop here
        if (start == -1) return false

        // If we have validated the mantissa, we can stop here if we've run out of characters
        if (start > endInclusive) return true
    }

    // Look for an exponent:
    //     - Mandatory for hexadecimal strings (marked by a p or P)
    //     - Optional for "regular" strings (marked by an e or E)
    var l = s[start++].asciiLetterToLowerCaseCode()
    if (l != if (isHex) 'p'.code else 'e'.code) {
        // We're here if the exponent character is not valid, but if the string is a "regular"
        // string, it could be a valid f/F/d/D suffix, so check for that (it must be the last
        // character too)
        return !isHex && (l == 'f'.code || l == 'd'.code) && start > endInclusive
    }

    // An exponent must be followed by digits
    if (start > endInclusive) return false

    // There may be a sign prefix before the exponent digits
    if (s[start] == '+' || s[start] == '-') {
        start++
        if (start > endInclusive) return false
    }

    // Look for digits after the exponent and its optional sign
    start = s.advanceWhile(start, endInclusive) { it.isAsciiDigit() }

    // The last suffix is optional, the string is valid here
    if (start > endInclusive) return true

    // We may have an optional fFdD suffix
    if (start == endInclusive) {
        l = s[start].asciiLetterToLowerCaseCode()
        return l == 'f'.code || l == 'd'.code
    }

    // Anything left is invalid
    return false
}

/**
 * Given a [start] and [endInclusive] index in a string, returns what possible float
 * named constant could be in that string. For instance, if there are 3 characters
 * between [start] and [endInclusive], this function will return "NaN".
 *
 * This function can return "NaN", "Infinity", or null. Null is returned when none of
 * the non-null constants can be stored in the string given the [start]/[endInclusive]
 * constraints.
 */
@kotlin.internal.InlineOnly
private inline fun guessNamedFloatConstant(start: Int, endInclusive: Int): String? = when (endInclusive) {
    start + 3 - 1 -> { // "NaN".length == 3, - 1 because we used and inclusive end index
        "NaN"
    }
    start + 8 - 1 -> { // "Infinity".length == 8, - 1 because we used and inclusive end index
        "Infinity"
    }
    else -> {
        // We have too many or too few characters, there's no valid constant
        null
    }
}

@kotlin.internal.InlineOnly
private inline fun Char.isAsciiDigit(): Boolean {
    // "and 0xFFFF" wraps negative values
    return (this - '0') and 0xFFFF < 10
}

@kotlin.internal.InlineOnly
private inline fun Char.isHexLetter(): Boolean {
    // "and 0xFFFF" wraps negative values
    return (asciiLetterToLowerCaseCode() - 'a'.code) and 0xFFFF < 6
}

/**
 * Speculatively transforms an upper-case ASCII character into its lower-case counterpart
 * and returns resulting code unit.
 *
 * The transformation is based on the fact that a difference between codes of
 * upper- and lower-case representations of the same ASCII letter is exactly 32.
 * So an upper-case letter could be transformed to a lower-case by adding 32.
 *
 * If [this] character lies outside the 'A'..'Z' range, a resulting code unit will not make much sense.
 * This function is not a general purpose solution for a case transformation,
 * and it is intended for use in conjunction with comparison,
 * like `'R'.asciiLetterToLowerCaseCode() == 'r'.code`.
 */
@kotlin.internal.InlineOnly
private inline fun Char.asciiLetterToLowerCaseCode(): Int = this.code or 0x20

@kotlin.internal.InlineOnly
private inline fun String.advanceWhile(start: Int, endInclusive: Int, predicate: (Char) -> Boolean): Int {
    var start = start
    while (start <= endInclusive && predicate(this[start])) start++
    return start
}

@kotlin.internal.InlineOnly
private inline fun String.backtrackWhile(start: Int, endInclusive: Int, predicate: (Char) -> Boolean): Int {
    var endInclusive = endInclusive
    while (endInclusive > start && predicate(this[endInclusive])) endInclusive--
    return endInclusive
}

/**
 * Advances until after the end of the mantissa, in the substring defined by the [start] and [endInclusive] indices.
 * If a valid mantissa cannot be found, this method returns -1.
 * If a valid mantissa is found, this method returns [endInclusive] + 1.
 */
@kotlin.internal.InlineOnly
private inline fun String.advanceAndValidateMantissa(start: Int, endInclusive: Int, hexFormat: Boolean, predicate: (Char) -> Boolean): Int {
    var start = start

    // Look for hex digits after the 0x prefix
    var checkpoint = start
    start = advanceWhile(start, endInclusive, predicate)

    // Check if we found the integer part of the number
    val hasIntegerPart = checkpoint != start

    // A hex string must have an exponent, the string is invalid if we only found an
    // integer part, but a non-hex string is valid if there's only an integer part
    if (start > endInclusive) return if (hexFormat) -1 else start

    var hasFractionalPart = false
    if (this[start] == '.') {
        start++

        // Look for hex digits for the fractional part
        checkpoint = start
        start = advanceWhile(start, endInclusive, predicate)

        // Did we find a fractional part?
        hasFractionalPart = checkpoint != start
    }

    // Both hex and non-hex strings must have an integer part, or a fractional part, or both
    if (!hasIntegerPart && !hasFractionalPart) {
        if (hexFormat) {
            return -1
        } else {
            // Check for non-finite constants
            val constant = guessNamedFloatConstant(start, endInclusive)
            if (constant == null) return -1

            // If the string contains exactly the constant we guessed, advance to after the constant
            return if (indexOf(constant, start, false) == start) endInclusive + 1 else -1
        }
    }

    return start
}
