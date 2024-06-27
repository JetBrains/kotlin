/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.text

import kotlin.internal.InlineOnly

// -------------------------- format and parse UByteArray --------------------------

/**
 * Formats bytes in this array using the specified [format].
 *
 * Note that only the [HexFormat.upperCase] and [HexFormat.bytes] properties of the [format] instance affect
 * the formatting result of this byte array.
 *
 * Each byte in the array is converted into two hexadecimal digits. The first digit represents the most significant 4 bits,
 * and the second digit represents the least significant 4 bits of the byte. The [HexFormat.upperCase] property determines whether
 * upper-case (`0-9`, `A-F`) or lower-case (`0-9`, `a-f`) hexadecimal digits are used for this conversion.
 * The [HexFormat.bytes] property specifies the strings that prefix and suffix each byte representation, and defines
 * how these representations are arranged.
 *
 * Refer to [HexFormat.BytesHexFormat] for details about the available format options and their impact on formatting.
 *
 * @param format the [HexFormat] to use for formatting, [HexFormat.Default] by default.
 * @return the result of formatting this array using the specified [format].
 *
 * @throws IllegalArgumentException if the result length exceeds the maximum capacity of [String].
 *
 * @sample samples.text.HexFormats.Extensions.byteArrayToHexString
 */
@ExperimentalStdlibApi
@SinceKotlin("1.9")
@ExperimentalUnsignedTypes
@InlineOnly
public inline fun UByteArray.toHexString(format: HexFormat = HexFormat.Default): String = storage.toHexString(format)

/**
 * Formats bytes in this array using the specified [format].
 *
 * Note that only the [HexFormat.upperCase] and [HexFormat.bytes] properties of the [format] instance affect
 * the formatting result of this byte array.
 *
 * Each byte in the array is converted into two hexadecimal digits. The first digit represents the most significant 4 bits,
 * and the second digit represents the least significant 4 bits of the byte. The [HexFormat.upperCase] property determines whether
 * upper-case (`0-9`, `A-F`) or lower-case (`0-9`, `a-f`) hexadecimal digits are used for this conversion.
 * The [HexFormat.bytes] property specifies the strings that prefix and suffix each byte representation, and defines
 * how these representations are arranged.
 *
 * Refer to [HexFormat.BytesHexFormat] for details about the available format options and their impact on formatting.
 *
 * @param startIndex the beginning (inclusive) of the subrange to format, 0 by default.
 * @param endIndex the end (exclusive) of the subrange to format, size of this array by default.
 * @param format the [HexFormat] to use for formatting, [HexFormat.Default] by default.
 * @return the result of formatting this array using the specified [format].
 *
 * @throws IndexOutOfBoundsException when [startIndex] or [endIndex] is out of range of this array indices.
 * @throws IllegalArgumentException when `startIndex > endIndex`.
 * @throws IllegalArgumentException if the result length exceeds the maximum capacity of [String].
 *
 * @sample samples.text.HexFormats.Extensions.byteArrayToHexString
 */
@ExperimentalStdlibApi
@SinceKotlin("1.9")
@ExperimentalUnsignedTypes
@InlineOnly
public inline fun UByteArray.toHexString(
    startIndex: Int = 0,
    endIndex: Int = size,
    format: HexFormat = HexFormat.Default
): String = storage.toHexString(startIndex, endIndex, format)

/**
 * Parses a byte array from this string using the specified [format].
 *
 * The string must conform to the structure defined by the [format].
 * Note that only the [HexFormat.bytes] property of the [format] instance affects the parsing of a byte array.
 *
 * Parsing is performed in a case-insensitive manner for both the hexadecimal digits and the elements
 * (prefix, suffix, separators) defined in the [HexFormat.bytes] property. Additionally, any of the
 * char sequences CRLF, LF and CR is considered a valid line separator.
 *
 * Refer to [HexFormat.BytesHexFormat] for details about the available format options and their impact on parsing.
 *
 * @param format the [HexFormat] to use for parsing, [HexFormat.Default] by default.
 * @return the byte array parsed from this string.
 *
 * @throws IllegalArgumentException if this string does not conform to the specified [format].
 *
 * @sample samples.text.HexFormats.Extensions.hexToByteArray
 */
@ExperimentalStdlibApi
@SinceKotlin("1.9")
@ExperimentalUnsignedTypes
@InlineOnly
public inline fun String.hexToUByteArray(format: HexFormat = HexFormat.Default): UByteArray =
    hexToByteArray(format).asUByteArray()

// -------------------------- format and parse UByte --------------------------

/**
 * Formats this `UByte` value using the specified [format].
 *
 * Note that only the [HexFormat.upperCase] and [HexFormat.number] properties of the [format] instance affect
 * the formatting result of this numeric value.
 *
 * This function converts the `UByte` value into its hexadecimal representation by mapping each four-bit chunk
 * of its binary representation to the corresponding hexadecimal digit, starting from the most significant bits.
 * The [HexFormat.upperCase] property determines whether upper-case (`0-9`, `A-F`) or lower-case (`0-9`, `a-f`)
 * hexadecimal digits are used for this conversion.
 * The [HexFormat.number] property adjusts the length of the hexadecimal representation by adding or removing
 * leading zeros as needed, and specifies the strings that prefix and suffix the resulting representation.
 *
 * Refer to [HexFormat.NumberHexFormat] for details about the available format options and their impact on formatting.
 *
 * @param format the [HexFormat] to use for formatting, [HexFormat.Default] by default.
 * @return the result of formatting this value using the specified [format].
 *
 * @sample samples.text.HexFormats.Extensions.byteToHexString
 */
@ExperimentalStdlibApi
@SinceKotlin("1.9")
@InlineOnly
public inline fun UByte.toHexString(format: HexFormat = HexFormat.Default): String = data.toHexString(format)

/**
 * Parses an `UByte` value from this string using the specified [format].
 *
 * The string must conform to the structure defined by the [format].
 * Note that only the [HexFormat.number] property of the [format] instance affects the parsing of a numeric value.
 *
 * The input string must start with the prefix and end with the suffix defined in the [HexFormat.number] property.
 * It must also contain at least one hexadecimal digit between them. If the number of hexadecimal digits
 * exceeds two, the excess leading digits must be zeros. This ensures that the value represented by the
 * hexadecimal digits fits into an 8-bit `UByte`.
 * Parsing is performed in a case-insensitive manner, including for the hexadecimal digits, prefix, and suffix.
 *
 * Refer to [HexFormat.NumberHexFormat] for details about the available format options and their impact on parsing.
 *
 * @param format the [HexFormat] to use for parsing, [HexFormat.Default] by default.
 * @return the `UByte` value parsed from this string.
 *
 * @throws IllegalArgumentException if this string does not conform to the specified [format], or if the hexadecimal
 *   digits represent a value that does not fit into an `UByte`.
 *
 * @sample samples.text.HexFormats.Extensions.hexToByte
 */
@ExperimentalStdlibApi
@SinceKotlin("1.9")
@InlineOnly
public inline fun String.hexToUByte(format: HexFormat = HexFormat.Default): UByte = hexToByte(format).toUByte()

// -------------------------- format and parse UShort --------------------------

/**
 * Formats this `UShort` value using the specified [format].
 *
 * Note that only the [HexFormat.upperCase] and [HexFormat.number] properties of the [format] instance affect
 * the formatting result of this numeric value.
 *
 * This function converts the `UShort` value into its hexadecimal representation by mapping each four-bit chunk
 * of its binary representation to the corresponding hexadecimal digit, starting from the most significant bits.
 * The [HexFormat.upperCase] property determines whether upper-case (`0-9`, `A-F`) or lower-case (`0-9`, `a-f`)
 * hexadecimal digits are used for this conversion.
 * The [HexFormat.number] property adjusts the length of the hexadecimal representation by adding or removing
 * leading zeros as needed, and specifies the strings that prefix and suffix the resulting representation.
 *
 * Refer to [HexFormat.NumberHexFormat] for details about the available format options and their impact on formatting.
 *
 * @param format the [HexFormat] to use for formatting, [HexFormat.Default] by default.
 * @return the result of formatting this value using the specified [format].
 *
 * @sample samples.text.HexFormats.Extensions.shortToHexString
 */
@ExperimentalStdlibApi
@SinceKotlin("1.9")
@InlineOnly
public inline fun UShort.toHexString(format: HexFormat = HexFormat.Default): String = data.toHexString(format)

/**
 * Parses an `UShort` value from this string using the specified [format].
 *
 * The string must conform to the structure defined by the [format].
 * Note that only the [HexFormat.number] property of the [format] instance affects the parsing of a numeric value.
 *
 * The input string must start with the prefix and end with the suffix defined in the [HexFormat.number] property.
 * It must also contain at least one hexadecimal digit between them. If the number of hexadecimal digits
 * exceeds four, the excess leading digits must be zeros. This ensures that the value represented by the
 * hexadecimal digits fits into a 16-bit `UShort`.
 * Parsing is performed in a case-insensitive manner, including for the hexadecimal digits, prefix, and suffix.
 *
 * Refer to [HexFormat.NumberHexFormat] for details about the available format options and their impact on parsing.
 *
 * @param format the [HexFormat] to use for parsing, [HexFormat.Default] by default.
 * @return the `UShort` value parsed from this string.
 *
 * @throws IllegalArgumentException if this string does not conform to the specified [format], or if the hexadecimal
 *   digits represent a value that does not fit into an `UShort`.
 *
 * @sample samples.text.HexFormats.Extensions.hexToShort
 */
@ExperimentalStdlibApi
@SinceKotlin("1.9")
@InlineOnly
public inline fun String.hexToUShort(format: HexFormat = HexFormat.Default): UShort = hexToShort(format).toUShort()

// -------------------------- format and parse UInt --------------------------

/**
 * Formats this `UInt` value using the specified [format].
 *
 * Note that only the [HexFormat.upperCase] and [HexFormat.number] properties of the [format] instance affect
 * the formatting result of this numeric value.
 *
 * This function converts the `UInt` value into its hexadecimal representation by mapping each four-bit chunk
 * of its binary representation to the corresponding hexadecimal digit, starting from the most significant bits.
 * The [HexFormat.upperCase] property determines whether upper-case (`0-9`, `A-F`) or lower-case (`0-9`, `a-f`)
 * hexadecimal digits are used for this conversion.
 * The [HexFormat.number] property adjusts the length of the hexadecimal representation by adding or removing
 * leading zeros as needed, and specifies the strings that prefix and suffix the resulting representation.
 *
 * Refer to [HexFormat.NumberHexFormat] for details about the available format options and their impact on formatting.
 *
 * @param format the [HexFormat] to use for formatting, [HexFormat.Default] by default.
 * @return the result of formatting this value using the specified [format].
 *
 * @sample samples.text.HexFormats.Extensions.intToHexString
 */
@ExperimentalStdlibApi
@SinceKotlin("1.9")
@InlineOnly
public inline fun UInt.toHexString(format: HexFormat = HexFormat.Default): String = data.toHexString(format)

/**
 * Parses an `UInt` value from this string using the specified [format].
 *
 * The string must conform to the structure defined by the [format].
 * Note that only the [HexFormat.number] property of the [format] instance affects the parsing of a numeric value.
 *
 * The input string must start with the prefix and end with the suffix defined in the [HexFormat.number] property.
 * It must also contain at least one hexadecimal digit between them. If the number of hexadecimal digits
 * exceeds eight, the excess leading digits must be zeros. This ensures that the value represented by the
 * hexadecimal digits fits into a 32-bit `UInt`.
 * Parsing is performed in a case-insensitive manner, including for the hexadecimal digits, prefix, and suffix.
 *
 * Refer to [HexFormat.NumberHexFormat] for details about the available format options and their impact on parsing.
 *
 * @param format the [HexFormat] to use for parsing, [HexFormat.Default] by default.
 * @return the `UInt` value parsed from this string.
 *
 * @throws IllegalArgumentException if this string does not conform to the specified [format], or if the hexadecimal
 *   digits represent a value that does not fit into an `UInt`.
 *
 * @sample samples.text.HexFormats.Extensions.hexToInt
 */
@ExperimentalStdlibApi
@SinceKotlin("1.9")
@InlineOnly
public inline fun String.hexToUInt(format: HexFormat = HexFormat.Default): UInt = hexToInt(format).toUInt()

// -------------------------- format and parse ULong --------------------------

/**
 * Formats this `ULong` value using the specified [format].
 *
 * Note that only the [HexFormat.upperCase] and [HexFormat.number] properties of the [format] instance affect
 * the formatting result of this numeric value.
 *
 * This function converts the `ULong` value into its hexadecimal representation by mapping each four-bit chunk
 * of its binary representation to the corresponding hexadecimal digit, starting from the most significant bits.
 * The [HexFormat.upperCase] property determines whether upper-case (`0-9`, `A-F`) or lower-case (`0-9`, `a-f`)
 * hexadecimal digits are used for this conversion.
 * The [HexFormat.number] property adjusts the length of the hexadecimal representation by adding or removing
 * leading zeros as needed, and specifies the strings that prefix and suffix the resulting representation.
 *
 * Refer to [HexFormat.NumberHexFormat] for details about the available format options and their impact on formatting.
 *
 * @param format the [HexFormat] to use for formatting, [HexFormat.Default] by default.
 * @return the result of formatting this value using the specified [format].
 *
 * @sample samples.text.HexFormats.Extensions.longToHexString
 */
@ExperimentalStdlibApi
@SinceKotlin("1.9")
@InlineOnly
public inline fun ULong.toHexString(format: HexFormat = HexFormat.Default): String = data.toHexString(format)

/**
 * Parses an `ULong` value from this string using the specified [format].
 *
 * The string must conform to the structure defined by the [format].
 * Note that only the [HexFormat.number] property of the [format] instance affects the parsing of a numeric value.
 *
 * The input string must start with the prefix and end with the suffix defined in the [HexFormat.number] property.
 * It must also contain at least one hexadecimal digit between them. If the number of hexadecimal digits
 * exceeds 16, the excess leading digits must be zeros. This ensures that the value represented by the
 * hexadecimal digits fits into a 64-bit `ULong`.
 * Parsing is performed in a case-insensitive manner, including for the hexadecimal digits, prefix, and suffix.
 *
 * Refer to [HexFormat.NumberHexFormat] for details about the available format options and their impact on parsing.
 *
 * @param format the [HexFormat] to use for parsing, [HexFormat.Default] by default.
 * @return the `ULong` value parsed from this string.
 *
 * @throws IllegalArgumentException if this string does not conform to the specified [format], or if the hexadecimal
 *   digits represent a value that does not fit into an `ULong`.
 *
 * @sample samples.text.HexFormats.Extensions.hexToLong
 */
@ExperimentalStdlibApi
@SinceKotlin("1.9")
@InlineOnly
public inline fun String.hexToULong(format: HexFormat = HexFormat.Default): ULong = hexToLong(format).toULong()
