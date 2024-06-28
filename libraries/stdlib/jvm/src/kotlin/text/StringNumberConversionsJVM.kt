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

/**
 * Recommended floating point number validation RegEx from the javadoc of `java.lang.Double.valueOf(String)`
 */
private object ScreenFloatValueRegEx {
    @JvmField val value = run {
        val Digits = "(\\p{Digit}+)"
        val HexDigits = "(\\p{XDigit}+)"
        val Exp = "[eE][+-]?$Digits"

        val HexString = "(0[xX]$HexDigits(\\.)?)|" + // 0[xX] HexDigits ._opt BinaryExponent FloatTypeSuffix_opt
                "(0[xX]$HexDigits?(\\.)$HexDigits)"  // 0[xX] HexDigits_opt . HexDigits BinaryExponent FloatTypeSuffix_opt

        val Number = "($Digits(\\.)?($Digits?)($Exp)?)|" +  // Digits ._opt Digits_opt ExponentPart_opt FloatTypeSuffix_opt
                "(\\.($Digits)($Exp)?)|" +                  // . Digits ExponentPart_opt FloatTypeSuffix_opt
                "(($HexString)[pP][+-]?$Digits)"            // HexSignificand BinaryExponent

        val fpRegex = "[\\x00-\\x20]*[+-]?(NaN|Infinity|(($Number)[fFdD]?))[\\x00-\\x20]*"

        Regex(fpRegex)
    }
}

private inline fun <T> screenFloatValue(str: String, parse: (String) -> T): T? {
    return try {
        if (ScreenFloatValueRegEx.value.matches(str))
            parse(str)
        else
            null
    } catch (e: NumberFormatException) {  // overflow
        null
    }
}
