/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package kotlin.text

/**
 * Returns a string representation of this [Byte] value in the specified [radix].
 *
 * @throws IllegalArgumentException when [radix] is not a valid radix for number to string conversion.
 */
@SinceKotlin("1.3")
@ExperimentalUnsignedTypes
//@kotlin.internal.InlineOnly
public /*inline*/ fun UByte.toString(radix: Int): String = this.toInt().toString(radix)

/**
 * Returns a string representation of this [Short] value in the specified [radix].
 *
 * @throws IllegalArgumentException when [radix] is not a valid radix for number to string conversion.
 */
@SinceKotlin("1.3")
@ExperimentalUnsignedTypes
//@kotlin.internal.InlineOnly
public /*inline*/ fun UShort.toString(radix: Int): String = this.toInt().toString(radix)


/**
 * Returns a string representation of this [Int] value in the specified [radix].
 *
 * @throws IllegalArgumentException when [radix] is not a valid radix for number to string conversion.
 */
@SinceKotlin("1.3")
@ExperimentalUnsignedTypes
//@kotlin.internal.InlineOnly
public /*inline*/ fun UInt.toString(radix: Int): String = this.toLong().toString(radix)

/**
 * Returns a string representation of this [Long] value in the specified [radix].
 *
 * @throws IllegalArgumentException when [radix] is not a valid radix for number to string conversion.
 */
@SinceKotlin("1.3")
@ExperimentalUnsignedTypes
public fun ULong.toString(radix: Int): String = ulongToString(this.toLong(), checkRadix(radix))


/**
 * Parses the string as a signed [UByte] number and returns the result.
 * @throws NumberFormatException if the string is not a valid representation of a number.
 */
@SinceKotlin("1.3")
@ExperimentalUnsignedTypes
public fun String.toUByte(): UByte = toUByteOrNull() ?: numberFormatError(this)

/**
 * Parses the string as a signed [UByte] number and returns the result.
 * @throws NumberFormatException if the string is not a valid representation of a number.
 * @throws IllegalArgumentException when [radix] is not a valid radix for string to number conversion.
 */
@SinceKotlin("1.3")
@ExperimentalUnsignedTypes
public fun String.toUByte(radix: Int): UByte = toUByteOrNull(radix) ?: numberFormatError(this)


/**
 * Parses the string as a [UShort] number and returns the result.
 * @throws NumberFormatException if the string is not a valid representation of a number.
 */
@SinceKotlin("1.3")
@ExperimentalUnsignedTypes
public fun String.toUShort(): UShort = toUShortOrNull() ?: numberFormatError(this)

/**
 * Parses the string as a [UShort] number and returns the result.
 * @throws NumberFormatException if the string is not a valid representation of a number.
 * @throws IllegalArgumentException when [radix] is not a valid radix for string to number conversion.
 */
@SinceKotlin("1.3")
@ExperimentalUnsignedTypes
public fun String.toUShort(radix: Int): UShort = toUShortOrNull(radix) ?: numberFormatError(this)

/**
 * Parses the string as an [UInt] number and returns the result.
 * @throws NumberFormatException if the string is not a valid representation of a number.
 */
@SinceKotlin("1.3")
@ExperimentalUnsignedTypes
public fun String.toUInt(): UInt = toUIntOrNull() ?: numberFormatError(this)

/**
 * Parses the string as an [UInt] number and returns the result.
 * @throws NumberFormatException if the string is not a valid representation of a number.
 * @throws IllegalArgumentException when [radix] is not a valid radix for string to number conversion.
 */
@SinceKotlin("1.3")
@ExperimentalUnsignedTypes
public fun String.toUInt(radix: Int): UInt = toUIntOrNull(radix) ?: numberFormatError(this)

/**
 * Parses the string as a [ULong] number and returns the result.
 * @throws NumberFormatException if the string is not a valid representation of a number.
 */
@SinceKotlin("1.3")
@ExperimentalUnsignedTypes
public fun String.toULong(): ULong = toULongOrNull() ?: numberFormatError(this)

/**
 * Parses the string as a [ULong] number and returns the result.
 * @throws NumberFormatException if the string is not a valid representation of a number.
 * @throws IllegalArgumentException when [radix] is not a valid radix for string to number conversion.
 */
@SinceKotlin("1.3")
@ExperimentalUnsignedTypes
public fun String.toULong(radix: Int): ULong = toULongOrNull(radix) ?: numberFormatError(this)





/**
 * Parses the string as an [UByte] number and returns the result
 * or `null` if the string is not a valid representation of a number.
 */
@SinceKotlin("1.3")
@ExperimentalUnsignedTypes
public fun String.toUByteOrNull(): UByte? = toUByteOrNull(radix = 10)

/**
 * Parses the string as an [UByte] number and returns the result
 * or `null` if the string is not a valid representation of a number.
 *
 * @throws IllegalArgumentException when [radix] is not a valid radix for string to number conversion.
 */
@SinceKotlin("1.3")
@ExperimentalUnsignedTypes
public fun String.toUByteOrNull(radix: Int): UByte? {
    val int = this.toUIntOrNull(radix) ?: return null
    if (int > UByte.MAX_VALUE) return null
    return int.toUByte()
}

/**
 * Parses the string as an [UShort] number and returns the result
 * or `null` if the string is not a valid representation of a number.
 */
@SinceKotlin("1.3")
@ExperimentalUnsignedTypes
public fun String.toUShortOrNull(): UShort? = toUShortOrNull(radix = 10)

/**
 * Parses the string as an [UShort] number and returns the result
 * or `null` if the string is not a valid representation of a number.
 *
 * @throws IllegalArgumentException when [radix] is not a valid radix for string to number conversion.
 */
@SinceKotlin("1.3")
@ExperimentalUnsignedTypes
public fun String.toUShortOrNull(radix: Int): UShort? {
    val int = this.toUIntOrNull(radix) ?: return null
    if (int > UShort.MAX_VALUE) return null
    return int.toUShort()
}

/**
 * Parses the string as an [UInt] number and returns the result
 * or `null` if the string is not a valid representation of a number.
 */
@SinceKotlin("1.3")
@ExperimentalUnsignedTypes
public fun String.toUIntOrNull(): UInt? = toUIntOrNull(radix = 10)

/**
 * Parses the string as an [UInt] number and returns the result
 * or `null` if the string is not a valid representation of a number.
 *
 * @throws IllegalArgumentException when [radix] is not a valid radix for string to number conversion.
 */
@SinceKotlin("1.3")
@ExperimentalUnsignedTypes
public fun String.toUIntOrNull(radix: Int): UInt? {
    checkRadix(radix)

    val length = this.length
    if (length == 0) return null

    val limit: UInt = UInt.MAX_VALUE
    val start: Int

    val firstChar = this[0]
    if (firstChar < '0') {
        if (length == 1 || firstChar != '+') return null
        start = 1
    } else {
        start = 0
    }

    val uradix = radix.toUInt()
    val limitBeforeMul = limit / uradix
    var result = 0u
    for (i in start until length) {
        val digit = digitOf(this[i], radix)

        if (digit < 0) return null
        if (result > limitBeforeMul) return null

        result *= uradix

        val beforeAdding = result
        result += digit.toUInt()
        if (result < beforeAdding) return null // overflow has happened
    }

    return result
}

/**
 * Parses the string as an [ULong] number and returns the result
 * or `null` if the string is not a valid representation of a number.
 */
@SinceKotlin("1.3")
@ExperimentalUnsignedTypes
public fun String.toULongOrNull(): ULong? = toULongOrNull(radix = 10)

/**
 * Parses the string as an [ULong] number and returns the result
 * or `null` if the string is not a valid representation of a number.
 *
 * @throws IllegalArgumentException when [radix] is not a valid radix for string to number conversion.
 */
@SinceKotlin("1.3")
@ExperimentalUnsignedTypes
public fun String.toULongOrNull(radix: Int): ULong? {
    checkRadix(radix)

    val length = this.length
    if (length == 0) return null

    val limit: ULong = ULong.MAX_VALUE
    val start: Int

    val firstChar = this[0]
    if (firstChar < '0') {
        if (length == 1 || firstChar != '+') return null
        start = 1
    } else {
        start = 0
    }


    val uradix = radix.toUInt()
    val limitBeforeMul = limit / uradix
    var result = 0uL
    for (i in start until length) {
        val digit = digitOf(this[i], radix)

        if (digit < 0) return null
        if (result > limitBeforeMul) return null

        result *= uradix

        val beforeAdding = result
        result += digit.toUInt()
        if (result < beforeAdding) return null // overflow has happened
    }

    return result
}
