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
    /* the code is somewhat ugly in order to achieve maximum performance */

    val len = this.length
    if (len == 0) return null

    val start: Int
    val negative: Boolean
    val limit: Int

    val firstChar = this[0]
    if (firstChar < '0') {  // Possible leading "+" or "-"
        start = 1

        if (firstChar == '-') {
            negative = true
            limit = Integer.MIN_VALUE
        } else if (firstChar == '+') {
            negative = false
            limit = -Integer.MAX_VALUE
        } else
            return null

        if (len == 1) return null  // Cannot have lone "+" or "-"

    } else {
        start = 0
        negative = false
        limit = -Integer.MAX_VALUE
    }


    val multmin = limit / 10
    var result = 0
    for (i in start..len - 1) {
        // Accumulating negatively avoids surprises near MAX_VALUE
        val digit = Character.digit(this[i], 10)

        if (digit < 0) return null
        if (result < multmin) return null

        result *= 10

        if (result < limit + digit) return null

        result -= digit
    }

    return if (negative) result else -result
}


/**
 * Parses the string as a [Long] number and returns the result
 * or `null` if the string is not a valid representation of a number.
 */
public fun String.toLongOrNull(): Long? {
    /* the code is somewhat ugly in order to achieve maximum performance */

    val len = this.length
    if (len == 0) return null

    val start: Int
    val negative: Boolean
    val limit: Long

    val firstChar = this[0]
    if (firstChar < '0') {  // Possible leading "+" or "-"
        start = 1

        if (firstChar == '-') {
            negative = true
            limit = Long.MIN_VALUE
        } else if (firstChar == '+') {
            negative = false
            limit = -Long.MAX_VALUE
        } else
            return null

        if (len == 1) return null  // Cannot have lone "+" or "-"

    } else {
        start = 0
        negative = false
        limit = -Long.MAX_VALUE
    }


    val multmin = limit / 10L
    var result = 0L
    for (i in start..len - 1) {
        // Accumulating negatively avoids surprises near MAX_VALUE
        val digit = Character.digit(this[i], 10)

        if (digit < 0) return null
        if (result < multmin) return null

        result *= 10

        if (result < limit + digit) return null

        result -= digit
    }

    return if (negative) result else -result
}

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

