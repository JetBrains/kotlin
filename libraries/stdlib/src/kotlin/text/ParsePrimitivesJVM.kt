@file:kotlin.jvm.JvmMultifileClass
@file:kotlin.jvm.JvmName("StringsKt")
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
@kotlin.internal.InlineOnly
public inline fun String.toByteOrNull(): Byte? = try {
    java.lang.Byte.parseByte(this)
} catch(e: NumberFormatException) { null }

/**
 * Parses the string as a [Short] number and returns the result
 * or `null` if the string is not a valid representation of a number.
 */
@kotlin.internal.InlineOnly
public inline fun String.toShortOrNull(): Short? = try {
    java.lang.Short.parseShort(this)
} catch(e: NumberFormatException) { null }

/**
 * Parses the string as an [Int] number and returns the result
 * or `null` if the string is not a valid representation of a number.
 */
public fun String.toIntOrNull(): Int? {
    val radix = 10

    var result = 0
    var negative = false
    var i = 0
    val len = this.length
    var limit = -Integer.MAX_VALUE
    val multmin: Int
    var digit: Int

    if (len > 0) {
        val firstChar = this[0]
        if (firstChar < '0') {
            // Possible leading "+" or "-"
            if (firstChar == '-') {
                negative = true
                limit = Integer.MIN_VALUE
            } else if (firstChar != '+')
                return null

            if (len == 1)
            // Cannot have lone "+" or "-"
                return null
            i++
        }
        multmin = limit / radix
        while (i < len) {
            // Accumulating negatively avoids surprises near MAX_VALUE
            digit = Character.digit(this[i++], radix)
            if (digit < 0) {
                return null
            }
            if (result < multmin) {
                return null
            }
            result *= radix
            if (result < limit + digit) {
                return null
            }
            result -= digit
        }
    } else {
        return null
    }
    return if (negative) result else -result
}


/**
 * Parses the string as a [Long] number and returns the result
 * or `null` if the string is not a valid representation of a number.
 */
@kotlin.internal.InlineOnly
public inline fun String.toLongOrNull(): Long? = try {
    java.lang.Long.parseLong(this)
} catch(e: NumberFormatException) { null }

/**
 * Parses the string as a [Float] number and returns the result
 * or `null` if the string is not a valid representation of a number.
 */
@kotlin.internal.InlineOnly
public inline fun String.toFloatOrNull(): Float? = try {
    java.lang.Float.parseFloat(this)
} catch(e: NumberFormatException) { null }

/**
 * Parses the string as a [Double] number and returns the result
 * or `null` if the string is not a valid representation of a number.
 */
@kotlin.internal.InlineOnly
public inline fun String.toDoubleOrNull(): Double? = try {
    java.lang.Double.parseDouble(this)
} catch(e: NumberFormatException) { null }

