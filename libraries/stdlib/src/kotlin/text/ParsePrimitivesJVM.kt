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
@kotlin.internal.InlineOnly
public inline fun String.toIntOrNull(): Int? = try {
    java.lang.Integer.parseInt(this)
} catch(e: NumberFormatException) { null }

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

