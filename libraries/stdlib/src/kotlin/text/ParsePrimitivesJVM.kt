@file:kotlin.jvm.JvmMultifileClass
@file:kotlin.jvm.JvmName("StringsKt")
@file:kotlin.jvm.JvmVersion
@file:Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")

package kotlin.text


/**
 * Returns `true` if the contents of this string is equal to the word "true", ignoring case, and `false` otherwise.
 */
@kotlin.internal.InlineOnly
public inline fun String.toBoolean(): Boolean = java.lang.Boolean.parseBoolean(this)

/**
 * Parses the string as a signed [Byte] number and returns the result.
 * @throws NumberFormatException if the string is not a valid representation of a number.
 */
@kotlin.internal.InlineOnly
public inline fun String.toByte(): Byte = java.lang.Byte.parseByte(this)

/**
 * Parses the string as a [Short] number and returns the result.
 * @throws NumberFormatException if the string is not a valid representation of a number.
 */
@kotlin.internal.InlineOnly
public inline fun String.toShort(): Short = java.lang.Short.parseShort(this)

/**
 * Parses the string as an [Int] number and returns the result.
 * @throws NumberFormatException if the string is not a valid representation of a number.
 */
@kotlin.internal.InlineOnly
public inline fun String.toInt(): Int = java.lang.Integer.parseInt(this)

/**
 * Parses the string as a [Long] number and returns the result.
 * @throws NumberFormatException if the string is not a valid representation of a number.
 */
@kotlin.internal.InlineOnly
public inline fun String.toLong(): Long = java.lang.Long.parseLong(this)

/**
 * Parses the string as a [Float] number and returns the result.
 * @throws NumberFormatException if the string is not a valid representation of a number.
 */
@kotlin.internal.InlineOnly
public inline fun String.toFloat(): Float = java.lang.Float.parseFloat(this)

/**
 * Parses the string as a [Double] number and returns the result.
 * @throws NumberFormatException if the string is not a valid representation of a number.
 */
@kotlin.internal.InlineOnly
public inline fun String.toDouble(): Double = java.lang.Double.parseDouble(this)



/**
 * Parses the string as a signed [Byte] number and returns the result
 * or `null` if the string is not a valid representation of a number.
 */
public fun String.toByteOrNull(): Byte? {
    val int = this.toIntOrNull() ?: return null
    if (int < Byte.MIN_VALUE || int > Byte.MAX_VALUE) return null
    return int.toByte()
}

/**
 * Parses the string as a [Short] number and returns the result
 * or `null` if the string is not a valid representation of a number.
 */
public fun String.toShortOrNull(): Short? {
    val int = this.toIntOrNull() ?: return null
    if (int < Short.MIN_VALUE || int > Short.MAX_VALUE) return null
    return int.toShort()
}

/**
 * Parses the string as an [Int] number and returns the result
 * or `null` if the string is not a valid representation of a number.
 */
public fun String.toIntOrNull(): Int? {
    val length = this.length
    if (length == 0) return null

    val start: Int
    val isNegative: Boolean
    val limit: Int

    val firstChar = this[0]
    if (firstChar < '0') {  // Possible leading sign
        if (length == 1) return null  // non-digit (possible sign) only, no digits after

        start = 1

        if (firstChar == '-') {
            isNegative = true
            limit = Int.MIN_VALUE
        } else if (firstChar == '+') {
            isNegative = false
            limit = -Int.MAX_VALUE
        } else
            return null
    } else {
        start = 0
        isNegative = false
        limit = -Int.MAX_VALUE
    }


    val limitBeforeMul = limit / 10
    var result = 0
    for (i in start..(length - 1)) {
        val digit = Character.digit(this[i], 10)

        if (digit < 0) return null
        if (result < limitBeforeMul) return null

        result *= 10

        if (result < limit + digit) return null

        result -= digit
    }

    return if (isNegative) result else -result
}

/**
 * Parses the string as a [Long] number and returns the result
 * or `null` if the string is not a valid representation of a number.
 */
public fun String.toLongOrNull(): Long? {
    val length = this.length
    if (length == 0) return null

    val start: Int
    val isNegative: Boolean
    val limit: Long

    val firstChar = this[0]
    if (firstChar < '0') {  // Possible leading sign
        if (length == 1) return null  // non-digit (possible sign) only, no digits after

        start = 1

        if (firstChar == '-') {
            isNegative = true
            limit = Long.MIN_VALUE
        } else if (firstChar == '+') {
            isNegative = false
            limit = -Long.MAX_VALUE
        } else
            return null
    } else {
        start = 0
        isNegative = false
        limit = -Long.MAX_VALUE
    }


    val limitBeforeMul = limit / 10L
    var result = 0L
    for (i in start..(length - 1)) {
        val digit = Character.digit(this[i], 10)

        if (digit < 0) return null
        if (result < limitBeforeMul) return null

        result *= 10

        if (result < limit + digit) return null

        result -= digit
    }

    return if (isNegative) result else -result
}

/**
 * Parses the string as a [Float] number and returns the result
 * or `null` if the string is not a valid representation of a number.
 */
public fun String.toFloatOrNull(): Float? = screenFloatValue(this, java.lang.Float::parseFloat)

/**
 * Parses the string as a [Double] number and returns the result
 * or `null` if the string is not a valid representation of a number.
 */
public fun String.toDoubleOrNull(): Double? = screenFloatValue(this, java.lang.Double::parseDouble)

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
    // they say the RegEx screens all invalid cases, but who knows..
    return try {
        if (ScreenFloatValueRegEx.value.matches(str))
            parse(str)
        else
            null
    } catch(e: NumberFormatException) {
        null
    }
}

