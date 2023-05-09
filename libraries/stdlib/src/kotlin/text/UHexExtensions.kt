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
 * Note that only [HexFormat.upperCase] and [HexFormat.BytesHexFormat] affect formatting.
 *
 * @param format the [HexFormat] to use for formatting, [HexFormat.Default] by default.
 *
 * @throws IllegalArgumentException if the result length is more than [String] maximum capacity.
 */
@ExperimentalStdlibApi
@SinceKotlin("1.9")
@ExperimentalUnsignedTypes
@InlineOnly
public inline fun UByteArray.toHexString(format: HexFormat = HexFormat.Default): String = storage.toHexString(format)

/**
 * Formats bytes in this array using the specified [HexFormat].
 *
 * Note that only [HexFormat.upperCase] and [HexFormat.BytesHexFormat] affect formatting.
 *
 * @param startIndex the beginning (inclusive) of the subrange to format, 0 by default.
 * @param endIndex the end (exclusive) of the subrange to format, size of this array by default.
 * @param format the [HexFormat] to use for formatting, [HexFormat.Default] by default.
 *
 * @throws IndexOutOfBoundsException when [startIndex] or [endIndex] is out of range of this array indices.
 * @throws IllegalArgumentException when `startIndex > endIndex`.
 * @throws IllegalArgumentException if the result length is more than [String] maximum capacity.
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
 * Parses bytes from this string using the specified [HexFormat].
 *
 * Note that only [HexFormat.BytesHexFormat] affects parsing,
 * and parsing is performed in case-insensitive manner.
 * Also, any of the char sequences CRLF, LF and CR is considered a valid line separator.
 *
 * @param format the [HexFormat] to use for parsing, [HexFormat.Default] by default.
 *
 * @throws IllegalArgumentException if this string does not comply with the specified [format].
 */
@ExperimentalStdlibApi
@SinceKotlin("1.9")
@ExperimentalUnsignedTypes
@InlineOnly
public inline fun String.hexToUByteArray(format: HexFormat = HexFormat.Default): UByteArray =
    hexToByteArray(format).asUByteArray()

///**
// * Parses bytes from this string using the specified [HexFormat].
// *
// * Note that only [HexFormat.BytesHexFormat] affects parsing,
// * and parsing is performed in case-insensitive manner.
// * Also, any of the char sequences CRLF, LF and CR is considered a valid line separator.
// *
// * @param startIndex the beginning (inclusive) of the substring to parse, 0 by default.
// * @param endIndex the end (exclusive) of the substring to parse, length of this string by default.
// * @param format the [HexFormat] to use for parsing, [HexFormat.Default] by default.
// *
// * @throws IndexOutOfBoundsException when [startIndex] or [endIndex] is out of range of this string indices.
// * @throws IllegalArgumentException when `startIndex > endIndex`.
// * @throws IllegalArgumentException if the substring does not comply with the specified [format].
// */
//@ExperimentalStdlibApi
//@SinceKotlin("1.9")
//@ExperimentalUnsignedTypes
//@InlineOnly
//public inline fun String.hexToUByteArray(
//    startIndex: Int = 0,
//    endIndex: Int = length,
//    format: HexFormat = HexFormat.Default
//): UByteArray = hexToByteArray(startIndex, endIndex, format).asUByteArray()

// -------------------------- format and parse UByte --------------------------

/**
 * Formats this `UByte` value using the specified [format].
 *
 * Note that only [HexFormat.upperCase] and [HexFormat.NumberHexFormat] affect formatting.
 *
 * @param format the [HexFormat] to use for formatting, [HexFormat.Default] by default.
 */
@ExperimentalStdlibApi
@SinceKotlin("1.9")
@InlineOnly
public inline fun UByte.toHexString(format: HexFormat = HexFormat.Default): String = data.toHexString(format)

/**
 * Parses an `UByte` value from this string using the specified [format].
 *
 * Note that only [HexFormat.NumberHexFormat] affects parsing,
 * and parsing is performed in case-insensitive manner.
 *
 * @param format the [HexFormat] to use for parsing, [HexFormat.Default] by default.
 *
 * @throws IllegalArgumentException if this string does not comply with the specified [format].
 */
@ExperimentalStdlibApi
@SinceKotlin("1.9")
@InlineOnly
public inline fun String.hexToUByte(format: HexFormat = HexFormat.Default): UByte = hexToByte(format).toUByte()

///**
// * Parses an `UByte` value from this string using the specified [format].
// *
// * Note that only [HexFormat.NumberHexFormat] affects parsing,
// * and parsing is performed in case-insensitive manner.
// *
// * @param startIndex the beginning (inclusive) of the substring to parse, 0 by default.
// * @param endIndex the end (exclusive) of the substring to parse, length of this string by default.
// * @param format the [HexFormat] to use for parsing, [HexFormat.Default] by default.
// *
// * @throws IndexOutOfBoundsException when [startIndex] or [endIndex] is out of range of this string indices.
// * @throws IllegalArgumentException when `startIndex > endIndex`.
// * @throws IllegalArgumentException if the substring does not comply with the specified [format].
// */
//@ExperimentalStdlibApi
//@SinceKotlin("1.9")
//@InlineOnly
//public inline fun String.hexToUByte(startIndex: Int = 0, endIndex: Int = length, format: HexFormat = HexFormat.Default): UByte =
//    hexToByte(startIndex, endIndex, format).toUByte()

// -------------------------- format and parse UShort --------------------------

/**
 * Formats this `UShort` value using the specified [format].
 *
 * Note that only [HexFormat.upperCase] and [HexFormat.NumberHexFormat] affect formatting.
 *
 * @param format the [HexFormat] to use for formatting, [HexFormat.Default] by default.
 */
@ExperimentalStdlibApi
@SinceKotlin("1.9")
@InlineOnly
public inline fun UShort.toHexString(format: HexFormat = HexFormat.Default): String = data.toHexString(format)

/**
 * Parses an `UShort` value from this string using the specified [format].
 *
 * Note that only [HexFormat.NumberHexFormat] affects parsing,
 * and parsing is performed in case-insensitive manner.
 *
 * @param format the [HexFormat] to use for parsing, [HexFormat.Default] by default.
 *
 * @throws IllegalArgumentException if this string does not comply with the specified [format].
 */
@ExperimentalStdlibApi
@SinceKotlin("1.9")
@InlineOnly
public inline fun String.hexToUShort(format: HexFormat = HexFormat.Default): UShort = hexToShort(format).toUShort()

///**
// * Parses an `UShort` value from this string using the specified [format].
// *
// * Note that only [HexFormat.NumberHexFormat] affects parsing,
// * and parsing is performed in case-insensitive manner.
// *
// * @param startIndex the beginning (inclusive) of the substring to parse, 0 by default.
// * @param endIndex the end (exclusive) of the substring to parse, length of this string by default.
// * @param format the [HexFormat] to use for parsing, [HexFormat.Default] by default.
// *
// * @throws IndexOutOfBoundsException when [startIndex] or [endIndex] is out of range of this string indices.
// * @throws IllegalArgumentException when `startIndex > endIndex`.
// * @throws IllegalArgumentException if the substring does not comply with the specified [format].
// */
//@ExperimentalStdlibApi
//@SinceKotlin("1.9")
//@InlineOnly
//public inline fun String.hexToUShort(startIndex: Int = 0, endIndex: Int = length, format: HexFormat = HexFormat.Default): UShort =
//    hexToShort(startIndex, endIndex, format).toUShort()

// -------------------------- format and parse UInt --------------------------

/**
 * Formats this `UInt` value using the specified [format].
 *
 * Note that only [HexFormat.upperCase] and [HexFormat.NumberHexFormat] affect formatting.
 *
 * @param format the [HexFormat] to use for formatting, [HexFormat.Default] by default.
 */
@ExperimentalStdlibApi
@SinceKotlin("1.9")
@InlineOnly
public inline fun UInt.toHexString(format: HexFormat = HexFormat.Default): String = data.toHexString(format)

/**
 * Parses an `UInt` value from this string using the specified [format].
 *
 * Note that only [HexFormat.NumberHexFormat] affects parsing,
 * and parsing is performed in case-insensitive manner.
 *
 * @param format the [HexFormat] to use for parsing, [HexFormat.Default] by default.
 *
 * @throws IllegalArgumentException if this string does not comply with the specified [format].
 */
@ExperimentalStdlibApi
@SinceKotlin("1.9")
@InlineOnly
public inline fun String.hexToUInt(format: HexFormat = HexFormat.Default): UInt = hexToInt(format).toUInt()

///**
// * Parses an `UInt` value from this string using the specified [format].
// *
// * Note that only [HexFormat.NumberHexFormat] affects parsing,
// * and parsing is performed in case-insensitive manner.
// *
// * @param startIndex the beginning (inclusive) of the substring to parse, 0 by default.
// * @param endIndex the end (exclusive) of the substring to parse, length of this string by default.
// * @param format the [HexFormat] to use for parsing, [HexFormat.Default] by default.
// *
// * @throws IndexOutOfBoundsException when [startIndex] or [endIndex] is out of range of this string indices.
// * @throws IllegalArgumentException when `startIndex > endIndex`.
// * @throws IllegalArgumentException if the substring does not comply with the specified [format].
// */
//@ExperimentalStdlibApi
//@SinceKotlin("1.9")
//@InlineOnly
//public inline fun String.hexToUInt(startIndex: Int = 0, endIndex: Int = length, format: HexFormat = HexFormat.Default): UInt =
//    hexToInt(startIndex, endIndex, format).toUInt()

// -------------------------- format and parse ULong --------------------------

/**
 * Formats this `ULong` value using the specified [format].
 *
 * Note that only [HexFormat.upperCase] and [HexFormat.NumberHexFormat] affect formatting.
 *
 * @param format the [HexFormat] to use for formatting, [HexFormat.Default] by default.
 */
@ExperimentalStdlibApi
@SinceKotlin("1.9")
@InlineOnly
public inline fun ULong.toHexString(format: HexFormat = HexFormat.Default): String = data.toHexString(format)

/**
 * Parses an `ULong` value from this string using the specified [format].
 *
 * Note that only [HexFormat.NumberHexFormat] affects parsing,
 * and parsing is performed in case-insensitive manner.
 *
 * @param format the [HexFormat] to use for parsing, [HexFormat.Default] by default.
 *
 * @throws IllegalArgumentException if this string does not comply with the specified [format].
 */
@ExperimentalStdlibApi
@SinceKotlin("1.9")
@InlineOnly
public inline fun String.hexToULong(format: HexFormat = HexFormat.Default): ULong = hexToLong(format).toULong()

///**
// * Parses an `ULong` value from this string using the specified [format].
// *
// * Note that only [HexFormat.NumberHexFormat] affects parsing,
// * and parsing is performed in case-insensitive manner.
// *
// * @param startIndex the beginning (inclusive) of the substring to parse, 0 by default.
// * @param endIndex the end (exclusive) of the substring to parse, length of this string by default.
// * @param format the [HexFormat] to use for parsing, [HexFormat.Default] by default.
// *
// * @throws IndexOutOfBoundsException when [startIndex] or [endIndex] is out of range of this string indices.
// * @throws IllegalArgumentException when `startIndex > endIndex`.
// * @throws IllegalArgumentException if the substring does not comply with the specified [format].
// */
//@ExperimentalStdlibApi
//@SinceKotlin("1.9")
//@InlineOnly
//public inline fun String.hexToULong(startIndex: Int = 0, endIndex: Int = length, format: HexFormat = HexFormat.Default): ULong =
//    hexToLong(startIndex, endIndex, format).toULong()
