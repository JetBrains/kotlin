/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.text

import kotlin.native.concurrent.SharedImmutable

private const val LOWER_CASE_HEX_DIGITS = "0123456789abcdef"
private const val UPPER_CASE_HEX_DIGITS = "0123456789ABCDEF"

// case-insensitive parsing
@SharedImmutable
private val HEX_DIGITS_TO_DECIMAL = IntArray(128) { -1 }.apply {
    LOWER_CASE_HEX_DIGITS.forEachIndexed { index, char -> this[char.code] = index }
    UPPER_CASE_HEX_DIGITS.forEachIndexed { index, char -> this[char.code] = index }
}

// -------------------------- format and parse ByteArray --------------------------

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
public fun ByteArray.toHexString(format: HexFormat = HexFormat.Default): String = toHexString(0, size, format)

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
public fun ByteArray.toHexString(
    startIndex: Int = 0,
    endIndex: Int = size,
    format: HexFormat = HexFormat.Default
): String {
    AbstractList.checkBoundsIndexes(startIndex, endIndex, size)

    if (startIndex == endIndex) {
        return ""
    }

    val digits = if (format.upperCase) UPPER_CASE_HEX_DIGITS else LOWER_CASE_HEX_DIGITS

    val bytesFormat = format.bytes
    val bytesPerLine = bytesFormat.bytesPerLine
    val bytesPerGroup = bytesFormat.bytesPerGroup
    val bytePrefix = bytesFormat.bytePrefix
    val byteSuffix = bytesFormat.byteSuffix
    val byteSeparator = bytesFormat.byteSeparator
    val groupSeparator = bytesFormat.groupSeparator

    val formatLength = formattedStringLength(
        totalBytes = endIndex - startIndex,
        bytesPerLine,
        bytesPerGroup,
        groupSeparator.length,
        byteSeparator.length,
        bytePrefix.length,
        byteSuffix.length
    )

    var indexInLine = 0
    var indexInGroup = 0

    return buildString(formatLength) {
        for (i in startIndex until endIndex) {
            val byte = this@toHexString[i].toInt() and 0xFF

            if (indexInLine == bytesPerLine) {
                append('\n')
                indexInLine = 0
                indexInGroup = 0
            } else if (indexInGroup == bytesPerGroup) {
                append(groupSeparator)
                indexInGroup = 0
            }
            if (indexInGroup != 0) {
                append(byteSeparator)
            }

            append(bytePrefix)
            append(digits[byte shr 4])
            append(digits[byte and 0xF])
            append(byteSuffix)

            indexInGroup += 1
            indexInLine += 1
        }

        check(formatLength == length)
    }
}

// Declared internal for testing
internal fun formattedStringLength(
    totalBytes: Int,
    bytesPerLine: Int,
    bytesPerGroup: Int,
    groupSeparatorLength: Int,
    byteSeparatorLength: Int,
    bytePrefixLength: Int,
    byteSuffixLength: Int
): Int {
    require(totalBytes > 0)
    // By contract bytesPerLine and bytesPerGroup are > 0

    val lineSeparators = (totalBytes - 1) / bytesPerLine
    val groupSeparators = run {
        val groupSeparatorsPerLine = (bytesPerLine - 1) / bytesPerGroup
        val bytesInLastLine = (totalBytes % bytesPerLine).let { if (it == 0) bytesPerLine else it }
        val groupSeparatorsInLastLine = (bytesInLastLine - 1) / bytesPerGroup
        lineSeparators * groupSeparatorsPerLine + groupSeparatorsInLastLine
    }
    val byteSeparators = totalBytes - 1 - lineSeparators - groupSeparators

    // The max totalLength is achieved when
    // totalBytes, bytePrefix/Suffix/Separator.length = Int.MAX_VALUE.
    // The result is 3 * Int.MAX_VALUE * Int.MAX_VALUE + Int.MAX_VALUE,
    // which is > Long.MAX_VALUE, but < ULong.MAX_VALUE.

    val totalLength: Long = lineSeparators.toLong() /* * lineSeparator.length = 1 */ +
            groupSeparators.toLong() * groupSeparatorLength.toLong() +
            byteSeparators.toLong() * byteSeparatorLength.toLong() +
            totalBytes.toLong() * (bytePrefixLength.toLong() + 2L + byteSuffixLength.toLong())

    if (totalLength !in 0..Int.MAX_VALUE) {
        // TODO: Common OutOfMemoryError?
        throw IllegalArgumentException("The resulting string length is too big: ${totalLength.toULong()}")
    }

    return totalLength.toInt()
}

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
public fun String.hexToByteArray(format: HexFormat = HexFormat.Default): ByteArray = hexToByteArray(0, length, format)

/**
 * Parses bytes from this string using the specified [HexFormat].
 *
 * Note that only [HexFormat.BytesHexFormat] affects parsing,
 * and parsing is performed in case-insensitive manner.
 * Also, any of the char sequences CRLF, LF and CR is considered a valid line separator.
 *
 * @param startIndex the beginning (inclusive) of the substring to parse, 0 by default.
 * @param endIndex the end (exclusive) of the substring to parse, length of this string by default.
 * @param format the [HexFormat] to use for parsing, [HexFormat.Default] by default.
 *
 * @throws IndexOutOfBoundsException when [startIndex] or [endIndex] is out of range of this string indices.
 * @throws IllegalArgumentException when `startIndex > endIndex`.
 * @throws IllegalArgumentException if the substring does not comply with the specified [format].
 */
@ExperimentalStdlibApi
//@SinceKotlin("1.9")
private fun String.hexToByteArray(
    startIndex: Int = 0,
    endIndex: Int = length,
    format: HexFormat = HexFormat.Default
): ByteArray {
    AbstractList.checkBoundsIndexes(startIndex, endIndex, length)

    if (startIndex == endIndex) {
        return byteArrayOf()
    }

    val bytesFormat = format.bytes
    val bytesPerLine = bytesFormat.bytesPerLine
    val bytesPerGroup = bytesFormat.bytesPerGroup
    val bytePrefix = bytesFormat.bytePrefix
    val byteSuffix = bytesFormat.byteSuffix
    val byteSeparator = bytesFormat.byteSeparator
    val groupSeparator = bytesFormat.groupSeparator

    val resultCapacity = parsedByteArrayMaxSize(
        stringLength = endIndex - startIndex,
        bytesPerLine,
        bytesPerGroup,
        groupSeparator.length,
        byteSeparator.length,
        bytePrefix.length,
        byteSuffix.length
    )
    val result = ByteArray(resultCapacity)

    var i = startIndex
    var byteIndex = 0
    var indexInLine = 0
    var indexInGroup = 0

    while (i < endIndex) {
        if (indexInLine == bytesPerLine) {
            i = checkNewLineAt(i, endIndex)
            indexInLine = 0
            indexInGroup = 0
        } else if (indexInGroup == bytesPerGroup) {
            i = checkContainsAt(groupSeparator, i, endIndex, "group separator")
            indexInGroup = 0
        } else if (indexInGroup != 0) {
            i = checkContainsAt(byteSeparator, i, endIndex, "byte separator")
        }
        indexInLine += 1
        indexInGroup += 1

        i = checkContainsAt(bytePrefix, i, endIndex, "byte prefix")

        checkHexLength(i, (i + 2).coerceAtMost(endIndex), maxDigits = 2, requireMaxLength = true)

        result[byteIndex++] = ((decimalFromHexDigitAt(i++) shl 4) or decimalFromHexDigitAt(i++)).toByte()

        i = checkContainsAt(byteSuffix, i, endIndex, "byte suffix")
    }

    return if (byteIndex == result.size) result else result.copyOf(byteIndex)
}

// Declared internal for testing
internal fun parsedByteArrayMaxSize(
    stringLength: Int,
    bytesPerLine: Int,
    bytesPerGroup: Int,
    groupSeparatorLength: Int,
    byteSeparatorLength: Int,
    bytePrefixLength: Int,
    byteSuffixLength: Int
): Int {
    require(stringLength > 0)
    // By contract bytesPerLine and bytesPerGroup are > 0

    // The max charsPerSet is achieved when
    // bytesPerLine/Group, bytePrefix/Suffix/SeparatorLength = Int.MAX_VALUE.
    // The result is 3 * Int.MAX_VALUE * Int.MAX_VALUE + Int.MAX_VALUE,
    // which is > Long.MAX_VALUE, but < ULong.MAX_VALUE.

    val charsPerByte = bytePrefixLength + 2L + byteSuffixLength

    val charsPerGroup = charsPerSet(charsPerByte, bytesPerGroup, byteSeparatorLength)

    val charsPerLine = if (bytesPerLine <= bytesPerGroup) {
        charsPerSet(charsPerByte, bytesPerLine, byteSeparatorLength)
    } else {
        val groupsPerLine = bytesPerLine / bytesPerGroup
        var result = charsPerSet(charsPerGroup, groupsPerLine, groupSeparatorLength)
        val bytesPerLastGroupInLine = bytesPerLine % bytesPerGroup
        if (bytesPerLastGroupInLine != 0) {
            result += groupSeparatorLength
            result += charsPerSet(charsPerByte, bytesPerLastGroupInLine, byteSeparatorLength)
        }
        result
    }

    var numberOfChars = stringLength.toLong()

    // assume one-character line separator to maximize size
    val wholeLines = wholeElementsPerSet(numberOfChars, charsPerLine, 1)
    numberOfChars -= wholeLines * (charsPerLine + 1)

    val wholeGroupsInLastLine = wholeElementsPerSet(numberOfChars, charsPerGroup, groupSeparatorLength)
    numberOfChars -= wholeGroupsInLastLine * (charsPerGroup + groupSeparatorLength)

    val wholeBytesInLastGroup = wholeElementsPerSet(numberOfChars, charsPerByte, byteSeparatorLength)
    numberOfChars -= wholeBytesInLastGroup * (charsPerByte + byteSeparatorLength)

    // If numberOfChars is bigger than zero here:
    //   * CRLF might have been used as line separator
    //   * or there are dangling characters at the end of string
    // Anyhow, have a spare capacity to let parsing continue.
    // In case of dangling characters it will throw later on with a correct message.
    val spare = if (numberOfChars > 0L) 1 else 0

    // The number of parsed bytes will always fit into Int, each parsed byte consumes at least 2 chars of the input string.
    return ((wholeLines * bytesPerLine) + (wholeGroupsInLastLine * bytesPerGroup) + wholeBytesInLastGroup + spare).toInt()
}

private fun charsPerSet(charsPerElement: Long, elementsPerSet: Int, elementSeparatorLength: Int): Long {
    require(elementsPerSet > 0)
    return (charsPerElement * elementsPerSet) + (elementSeparatorLength * (elementsPerSet - 1L))
}

private fun wholeElementsPerSet(charsPerSet: Long, charsPerElement: Long, elementSeparatorLength: Int): Long {
    return if (charsPerSet <= 0 || charsPerElement <= 0) 0
    else (charsPerSet + elementSeparatorLength) / (charsPerElement + elementSeparatorLength)
}

private fun String.checkNewLineAt(index: Int, endIndex: Int): Int {
    return if (this[index] == '\r') {
        if (index + 1 < endIndex && this[index + 1] == '\n') index + 2 else index + 1
    } else if (this[index] == '\n') {
        index + 1
    } else {
        throw NumberFormatException("Expected a new line at index $index, but was ${this[index]}")
    }
}

// -------------------------- format and parse Byte --------------------------

/**
 * Formats this `Byte` value using the specified [format].
 *
 * Note that only [HexFormat.upperCase] and [HexFormat.NumberHexFormat] affect formatting.
 *
 * @param format the [HexFormat] to use for formatting, [HexFormat.Default] by default.
 */
@ExperimentalStdlibApi
@SinceKotlin("1.9")
public fun Byte.toHexString(format: HexFormat = HexFormat.Default): String = toLong().toHexStringImpl(format, bits = 8)

/**
 * Parses a `Byte` value from this string using the specified [format].
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
public fun String.hexToByte(format: HexFormat = HexFormat.Default): Byte = hexToByte(0, length, format)

/**
 * Parses a `Byte` value from this string using the specified [format].
 *
 * Note that only [HexFormat.NumberHexFormat] affects parsing,
 * and parsing is performed in case-insensitive manner.
 *
 * @param startIndex the beginning (inclusive) of the substring to parse, 0 by default.
 * @param endIndex the end (exclusive) of the substring to parse, length of this string by default.
 * @param format the [HexFormat] to use for parsing, [HexFormat.Default] by default.
 *
 * @throws IndexOutOfBoundsException when [startIndex] or [endIndex] is out of range of this string indices.
 * @throws IllegalArgumentException when `startIndex > endIndex`.
 * @throws IllegalArgumentException if the substring does not comply with the specified [format].
 */
@ExperimentalStdlibApi
//@SinceKotlin("1.9")
private fun String.hexToByte(startIndex: Int = 0, endIndex: Int = length, format: HexFormat = HexFormat.Default): Byte =
    hexToLongImpl(startIndex, endIndex, format, maxDigits = 2).toByte()

// -------------------------- format and parse Short --------------------------

/**
 * Formats this `Short` value using the specified [format].
 *
 * Note that only [HexFormat.upperCase] and [HexFormat.NumberHexFormat] affect formatting.
 *
 * @param format the [HexFormat] to use for formatting, [HexFormat.Default] by default.
 */
@ExperimentalStdlibApi
@SinceKotlin("1.9")
public fun Short.toHexString(format: HexFormat = HexFormat.Default): String = toLong().toHexStringImpl(format, bits = 16)

/**
 * Parses a `Short` value from this string using the specified [format].
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
public fun String.hexToShort(format: HexFormat = HexFormat.Default): Short = hexToShort(0, length, format)

/**
 * Parses a `Short` value from this string using the specified [format].
 *
 * Note that only [HexFormat.NumberHexFormat] affects parsing,
 * and parsing is performed in case-insensitive manner.
 *
 * @param startIndex the beginning (inclusive) of the substring to parse, 0 by default.
 * @param endIndex the end (exclusive) of the substring to parse, length of this string by default.
 * @param format the [HexFormat] to use for parsing, [HexFormat.Default] by default.
 *
 * @throws IndexOutOfBoundsException when [startIndex] or [endIndex] is out of range of this string indices.
 * @throws IllegalArgumentException when `startIndex > endIndex`.
 * @throws IllegalArgumentException if the substring does not comply with the specified [format].
 */
@ExperimentalStdlibApi
//@SinceKotlin("1.9")
private fun String.hexToShort(startIndex: Int = 0, endIndex: Int = length, format: HexFormat = HexFormat.Default): Short =
    hexToLongImpl(startIndex, endIndex, format, maxDigits = 4).toShort()

// -------------------------- format and parse Int --------------------------

/**
 * Formats this `Int` value using the specified [format].
 *
 * Note that only [HexFormat.upperCase] and [HexFormat.NumberHexFormat] affect formatting.
 *
 * @param format the [HexFormat] to use for formatting, [HexFormat.Default] by default.
 */
@ExperimentalStdlibApi
@SinceKotlin("1.9")
public fun Int.toHexString(format: HexFormat = HexFormat.Default): String = toLong().toHexStringImpl(format, bits = 32)

/**
 * Parses an `Int` value from this string using the specified [format].
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
public fun String.hexToInt(format: HexFormat = HexFormat.Default): Int = hexToInt(0, length, format)

/**
 * Parses an `Int` value from this string using the specified [format].
 *
 * Note that only [HexFormat.NumberHexFormat] affects parsing,
 * and parsing is performed in case-insensitive manner.
 *
 * @param startIndex the beginning (inclusive) of the substring to parse, 0 by default.
 * @param endIndex the end (exclusive) of the substring to parse, length of this string by default.
 * @param format the [HexFormat] to use for parsing, [HexFormat.Default] by default.
 *
 * @throws IndexOutOfBoundsException when [startIndex] or [endIndex] is out of range of this string indices.
 * @throws IllegalArgumentException when `startIndex > endIndex`.
 * @throws IllegalArgumentException if the substring does not comply with the specified [format].
 */
@ExperimentalStdlibApi
//@SinceKotlin("1.9")
private fun String.hexToInt(startIndex: Int = 0, endIndex: Int = length, format: HexFormat = HexFormat.Default): Int =
    hexToLongImpl(startIndex, endIndex, format, maxDigits = 8).toInt()

// -------------------------- format and parse Long --------------------------

/**
 * Formats this `Long` value using the specified [format].
 *
 * Note that only [HexFormat.upperCase] and [HexFormat.NumberHexFormat] affect formatting.
 *
 * @param format the [HexFormat] to use for formatting, [HexFormat.Default] by default.
 */
@ExperimentalStdlibApi
@SinceKotlin("1.9")
public fun Long.toHexString(format: HexFormat = HexFormat.Default): String = toHexStringImpl(format, bits = 64)

/**
 * Parses a `Long` value from this string using the specified [format].
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
public fun String.hexToLong(format: HexFormat = HexFormat.Default): Long = hexToLong(0, length, format)

/**
 * Parses a `Long` value from this string using the specified [format].
 *
 * Note that only [HexFormat.NumberHexFormat] affects parsing,
 * and parsing is performed in case-insensitive manner.
 *
 * @param startIndex the beginning (inclusive) of the substring to parse, 0 by default.
 * @param endIndex the end (exclusive) of the substring to parse, length of this string by default.
 * @param format the [HexFormat] to use for parsing, [HexFormat.Default] by default.
 *
 * @throws IndexOutOfBoundsException when [startIndex] or [endIndex] is out of range of this string indices.
 * @throws IllegalArgumentException when `startIndex > endIndex`.
 * @throws IllegalArgumentException if the substring does not comply with the specified [format].
 */
@ExperimentalStdlibApi
//@SinceKotlin("1.9")
private fun String.hexToLong(startIndex: Int = 0, endIndex: Int = length, format: HexFormat = HexFormat.Default): Long =
    hexToLongImpl(startIndex, endIndex, format, maxDigits = 16)

// -------------------------- private format and parse functions --------------------------

@ExperimentalStdlibApi
private fun Long.toHexStringImpl(format: HexFormat, bits: Int): String {
    require(bits and 0x3 == 0)

    val digits = if (format.upperCase) UPPER_CASE_HEX_DIGITS else LOWER_CASE_HEX_DIGITS
    val value = this

    val prefix = format.number.prefix
    val suffix = format.number.suffix
    val formatLength = prefix.length + (bits shr 2) + suffix.length
    var removeZeros = format.number.removeLeadingZeros

    return buildString(formatLength) {
        append(prefix)

        var shift = bits
        while (shift > 0) {
            shift -= 4
            val decimal = ((value shr shift) and 0xF).toInt()
            removeZeros = removeZeros && decimal == 0 && shift > 0
            if (!removeZeros) {
                append(digits[decimal])
            }
        }

        append(suffix)
    }
}

@ExperimentalStdlibApi
private fun String.hexToLongImpl(startIndex: Int = 0, endIndex: Int = length, format: HexFormat, maxDigits: Int): Long {
    AbstractList.checkBoundsIndexes(startIndex, endIndex, length)

    val prefix = format.number.prefix
    val suffix = format.number.suffix

    if (prefix.length + suffix.length >= endIndex - startIndex) {
        throw NumberFormatException(
            "Expected a hexadecimal number with prefix \"$prefix\" and suffix \"$suffix\", but was ${substring(startIndex, endIndex)}"
        )
    }

    val digitsStartIndex = checkContainsAt(prefix, startIndex, endIndex, "prefix")
    val digitsEndIndex = endIndex - suffix.length
    checkContainsAt(suffix, digitsEndIndex, endIndex, "suffix")

    checkHexLength(digitsStartIndex, digitsEndIndex, maxDigits, requireMaxLength = false)

    var result = 0L
    for (i in digitsStartIndex until digitsEndIndex) {
        result = (result shl 4) or decimalFromHexDigitAt(i).toLong()
    }
    return result
}

private fun String.checkContainsAt(part: String, index: Int, endIndex: Int, partName: String): Int {
    val end = index + part.length
    if (end > endIndex || !regionMatches(index, part, 0, part.length, ignoreCase = true)) {
        throw NumberFormatException(
            "Expected $partName \"$part\" at index $index, but was ${this.substring(index, end.coerceAtMost(endIndex))}"
        )
    }
    return end
}

private fun String.checkHexLength(startIndex: Int, endIndex: Int, maxDigits: Int, requireMaxLength: Boolean) {
    val digitsLength = endIndex - startIndex
    val isCorrectLength = if (requireMaxLength) digitsLength == maxDigits else digitsLength <= maxDigits
    if (!isCorrectLength) {
        val specifier = if (requireMaxLength) "exactly" else "at most"
        val substring = substring(startIndex, endIndex)
        throw NumberFormatException(
            "Expected $specifier $maxDigits hexadecimal digits at index $startIndex, but was $substring of length $digitsLength"
        )
    }
}

private fun String.decimalFromHexDigitAt(index: Int): Int {
    val code = this[index].code
    if (code > 127 || HEX_DIGITS_TO_DECIMAL[code] < 0) {
        throw NumberFormatException("Expected a hexadecimal digit at index $index, but was ${this[index]}")
    }
    return HEX_DIGITS_TO_DECIMAL[code]
}
