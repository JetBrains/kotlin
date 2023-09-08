/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.text

// Benchmarks repository: https://github.com/qurbonzoda/KotlinHexFormatBenchmarks

private const val LOWER_CASE_HEX_DIGITS = "0123456789abcdef"
private const val UPPER_CASE_HEX_DIGITS = "0123456789ABCDEF"

/**
 * The table for converting Byte values to their two-digit hex representation.
 *
 * It's used for formatting ByteArray. Storing the hex representation
 * of each Byte value makes it possible to access the table only once per Byte.
 * This noticeably improves performance, especially for large ByteArray's.
 */
private val BYTE_TO_LOWER_CASE_HEX_DIGITS = IntArray(256) {
    (LOWER_CASE_HEX_DIGITS[(it shr 4)].code shl 8) or LOWER_CASE_HEX_DIGITS[(it and 0xF)].code
}

/**
 * The table for converting Byte values to their two-digit hex representation.
 *
 * @see BYTE_TO_LOWER_CASE_HEX_DIGITS
 */
private val BYTE_TO_UPPER_CASE_HEX_DIGITS = IntArray(256) {
    (UPPER_CASE_HEX_DIGITS[(it shr 4)].code shl 8) or UPPER_CASE_HEX_DIGITS[(it and 0xF)].code
}

/**
 * The table for converting hex digits (both lowercase and uppercase) to their `Int` decimal value.
 *
 * Although `Char.code` of every hex digit is less than 128, the table size is 256 for the following reason:
 * If the string whose chars are being converted is ASCII-encoded, the JIT optimizes `charAt` to a byte-sized load.
 * When the table is 256 entries wide then both the `code ushr 8 == 0` and array bounds check are eliminated.
 * This noticeably improves performance for ASCII strings.
 */
private val HEX_DIGITS_TO_DECIMAL = IntArray(256) { -1 }.apply {
    LOWER_CASE_HEX_DIGITS.forEachIndexed { index, char -> this[char.code] = index }
    UPPER_CASE_HEX_DIGITS.forEachIndexed { index, char -> this[char.code] = index }
}

/**
 * The table for converting hex digits (both lowercase and uppercase) to their `Long` decimal value.
 *
 * Because `Int.toLong()` noticeably impacted performance, this separate table was introduced.
 *
 * @see HEX_DIGITS_TO_DECIMAL
 */
private val HEX_DIGITS_TO_LONG_DECIMAL = LongArray(256) { -1 }.apply {
    LOWER_CASE_HEX_DIGITS.forEachIndexed { index, char -> this[char.code] = index.toLong() }
    UPPER_CASE_HEX_DIGITS.forEachIndexed { index, char -> this[char.code] = index.toLong() }
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

    val byteToDigits = if (format.upperCase) BYTE_TO_UPPER_CASE_HEX_DIGITS else BYTE_TO_LOWER_CASE_HEX_DIGITS
    val bytesFormat = format.bytes

    // Optimize for formats with unspecified bytesPerLine and bytesPerGroup
    if (bytesFormat.noLineAndGroupSeparator) {
        return toHexStringNoLineAndGroupSeparator(startIndex, endIndex, bytesFormat, byteToDigits)
    }

    return toHexStringSlowPath(startIndex, endIndex, bytesFormat, byteToDigits)
}

@ExperimentalStdlibApi
private fun ByteArray.toHexStringNoLineAndGroupSeparator(
    startIndex: Int,
    endIndex: Int,
    bytesFormat: HexFormat.BytesHexFormat,
    byteToDigits: IntArray
): String {
    // Optimize for formats with a short byteSeparator and no bytePrefix/Suffix
    if (bytesFormat.shortByteSeparatorNoPrefixAndSuffix) {
        return toHexStringShortByteSeparatorNoPrefixAndSuffix(startIndex, endIndex, bytesFormat, byteToDigits)
    }

    return toHexStringNoLineAndGroupSeparatorSlowPath(startIndex, endIndex, bytesFormat, byteToDigits)
}

@ExperimentalStdlibApi
private fun ByteArray.toHexStringShortByteSeparatorNoPrefixAndSuffix(
    startIndex: Int,
    endIndex: Int,
    bytesFormat: HexFormat.BytesHexFormat,
    byteToDigits: IntArray
): String {
    val byteSeparatorLength = bytesFormat.byteSeparator.length
    require(byteSeparatorLength <= 1)

    val numberOfBytes = endIndex - startIndex
    var charIndex = 0

    if (byteSeparatorLength == 0) {
        val charArray = CharArray(checkFormatLength(2L * numberOfBytes))
        for (byteIndex in startIndex until endIndex) {
            charIndex = formatByteAt(byteIndex, byteToDigits, charArray, charIndex)
        }
        return charArray.concatToString()
    } else {
        val charArray = CharArray(checkFormatLength(3L * numberOfBytes - 1))
        val byteSeparatorChar = bytesFormat.byteSeparator[0]

        charIndex = formatByteAt(startIndex, byteToDigits, charArray, charIndex)
        for (byteIndex in startIndex + 1 until endIndex) {
            charArray[charIndex++] = byteSeparatorChar
            charIndex = formatByteAt(byteIndex, byteToDigits, charArray, charIndex)
        }

        return charArray.concatToString()
    }
}

@ExperimentalStdlibApi
private fun ByteArray.toHexStringNoLineAndGroupSeparatorSlowPath(
    startIndex: Int,
    endIndex: Int,
    bytesFormat: HexFormat.BytesHexFormat,
    byteToDigits: IntArray
): String {
    val bytePrefix = bytesFormat.bytePrefix
    val byteSuffix = bytesFormat.byteSuffix
    val byteSeparator = bytesFormat.byteSeparator

    val formatLength = formattedStringLength(
        numberOfBytes = endIndex - startIndex,
        byteSeparator.length,
        bytePrefix.length,
        byteSuffix.length
    )
    val charArray = CharArray(formatLength)
    var charIndex = 0

    charIndex = formatByteAt(startIndex, bytePrefix, byteSuffix, byteToDigits, charArray, charIndex)
    for (byteIndex in startIndex + 1 until endIndex) {
        charIndex = byteSeparator.toCharArrayIfNotEmpty(charArray, charIndex)
        charIndex = formatByteAt(byteIndex, bytePrefix, byteSuffix, byteToDigits, charArray, charIndex)
    }

    return charArray.concatToString()
}

@ExperimentalStdlibApi
private fun ByteArray.toHexStringSlowPath(
    startIndex: Int,
    endIndex: Int,
    bytesFormat: HexFormat.BytesHexFormat,
    byteToDigits: IntArray
): String {
    val bytesPerLine = bytesFormat.bytesPerLine
    val bytesPerGroup = bytesFormat.bytesPerGroup
    val bytePrefix = bytesFormat.bytePrefix
    val byteSuffix = bytesFormat.byteSuffix
    val byteSeparator = bytesFormat.byteSeparator
    val groupSeparator = bytesFormat.groupSeparator

    val formatLength = formattedStringLength(
        numberOfBytes = endIndex - startIndex,
        bytesPerLine,
        bytesPerGroup,
        groupSeparator.length,
        byteSeparator.length,
        bytePrefix.length,
        byteSuffix.length
    )
    val charArray = CharArray(formatLength)
    var charIndex = 0

    var indexInLine = 0
    var indexInGroup = 0

    for (byteIndex in startIndex until endIndex) {
        if (indexInLine == bytesPerLine) {
            charArray[charIndex++] = '\n'
            indexInLine = 0
            indexInGroup = 0
        } else if (indexInGroup == bytesPerGroup) {
            charIndex = groupSeparator.toCharArrayIfNotEmpty(charArray, charIndex)
            indexInGroup = 0
        }
        if (indexInGroup != 0) {
            charIndex = byteSeparator.toCharArrayIfNotEmpty(charArray, charIndex)
        }

        charIndex = formatByteAt(byteIndex, bytePrefix, byteSuffix, byteToDigits, charArray, charIndex)

        indexInGroup += 1
        indexInLine += 1
    }

    check(charIndex == formatLength)
    return charArray.concatToString()
}

private fun ByteArray.formatByteAt(
    index: Int,
    bytePrefix: String,
    byteSuffix: String,
    byteToDigits: IntArray,
    destination: CharArray,
    destinationOffset: Int
): Int {
    var offset = bytePrefix.toCharArrayIfNotEmpty(destination, destinationOffset)
    offset = formatByteAt(index, byteToDigits, destination, offset)
    return byteSuffix.toCharArrayIfNotEmpty(destination, offset)
}

private fun ByteArray.formatByteAt(
    index: Int,
    byteToDigits: IntArray,
    destination: CharArray,
    destinationOffset: Int
): Int {
    val byte = this[index].toInt() and 0xFF
    val byteDigits = byteToDigits[byte]
    destination[destinationOffset] = (byteDigits shr 8).toChar()
    destination[destinationOffset + 1] = (byteDigits and 0xFF).toChar()
    return destinationOffset + 2
}

private fun formattedStringLength(
    numberOfBytes: Int,
    byteSeparatorLength: Int,
    bytePrefixLength: Int,
    byteSuffixLength: Int
): Int {
    require(numberOfBytes > 0)

    val charsPerByte = 2L + bytePrefixLength + byteSuffixLength + byteSeparatorLength
    val formatLength = numberOfBytes * charsPerByte - byteSeparatorLength
    return checkFormatLength(formatLength)
}

// Declared internal for testing
internal fun formattedStringLength(
    numberOfBytes: Int,
    bytesPerLine: Int,
    bytesPerGroup: Int,
    groupSeparatorLength: Int,
    byteSeparatorLength: Int,
    bytePrefixLength: Int,
    byteSuffixLength: Int
): Int {
    require(numberOfBytes > 0)
    // By contract bytesPerLine and bytesPerGroup are > 0

    val lineSeparators = (numberOfBytes - 1) / bytesPerLine
    val groupSeparators = run {
        val groupSeparatorsPerLine = (bytesPerLine - 1) / bytesPerGroup
        val bytesInLastLine = (numberOfBytes % bytesPerLine).let { if (it == 0) bytesPerLine else it }
        val groupSeparatorsInLastLine = (bytesInLastLine - 1) / bytesPerGroup
        lineSeparators * groupSeparatorsPerLine + groupSeparatorsInLastLine
    }
    val byteSeparators = numberOfBytes - 1 - lineSeparators - groupSeparators

    // The max formatLength is achieved when
    // numberOfBytes, bytePrefix/Suffix/Separator.length = Int.MAX_VALUE.
    // The result is 3 * Int.MAX_VALUE * Int.MAX_VALUE + Int.MAX_VALUE,
    // which is > Long.MAX_VALUE, but < ULong.MAX_VALUE.

    val formatLength: Long = lineSeparators.toLong() /* * lineSeparator.length = 1 */ +
            groupSeparators.toLong() * groupSeparatorLength.toLong() +
            byteSeparators.toLong() * byteSeparatorLength.toLong() +
            numberOfBytes.toLong() * (bytePrefixLength.toLong() + 2L + byteSuffixLength.toLong())

    return checkFormatLength(formatLength)
}

private fun checkFormatLength(formatLength: Long): Int {
    if (formatLength !in 0..Int.MAX_VALUE) {
        // TODO: Common OutOfMemoryError?
        throw IllegalArgumentException("The resulting string length is too big: ${formatLength.toULong()}")
    }
    return formatLength.toInt()
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

    // Optimize for formats with unspecified bytesPerLine and bytesPerGroup
    if (bytesFormat.noLineAndGroupSeparator) {
        hexToByteArrayNoLineAndGroupSeparator(startIndex, endIndex, bytesFormat)?.let { return it }
    }

    return hexToByteArraySlowPath(startIndex, endIndex, bytesFormat)
}

@ExperimentalStdlibApi
private fun String.hexToByteArrayNoLineAndGroupSeparator(
    startIndex: Int,
    endIndex: Int,
    bytesFormat: HexFormat.BytesHexFormat
): ByteArray? {
    // Optimize for formats with a short byteSeparator and no bytePrefix/Suffix
    if (bytesFormat.shortByteSeparatorNoPrefixAndSuffix) {
        return hexToByteArrayShortByteSeparatorNoPrefixAndSuffix(startIndex, endIndex, bytesFormat)
    }

    return hexToByteArrayNoLineAndGroupSeparatorSlowPath(startIndex, endIndex, bytesFormat)
}

@ExperimentalStdlibApi
private fun String.hexToByteArrayShortByteSeparatorNoPrefixAndSuffix(
    startIndex: Int,
    endIndex: Int,
    bytesFormat: HexFormat.BytesHexFormat
): ByteArray? {
    val byteSeparatorLength = bytesFormat.byteSeparator.length
    require(byteSeparatorLength <= 1)

    val numberOfChars = endIndex - startIndex
    var charIndex = 0

    if (byteSeparatorLength == 0) {
        if (numberOfChars and 1 != 0) return null
        val numberOfBytes = numberOfChars shr 1
        val byteArray = ByteArray(numberOfBytes)
        for (byteIndex in 0 until numberOfBytes) {
            byteArray[byteIndex] = parseByteAt(charIndex)
            charIndex += 2
        }
        return byteArray
    } else {
        if (numberOfChars % 3 != 2) return null
        val numberOfBytes = numberOfChars / 3 + 1
        val byteArray = ByteArray(numberOfBytes)
        val byteSeparatorChar = bytesFormat.byteSeparator[0]
        byteArray[0] = parseByteAt(charIndex)
        charIndex += 2
        for (byteIndex in 1 until numberOfBytes) {
            if (this[charIndex] != byteSeparatorChar) {
                checkContainsAt(charIndex, endIndex, bytesFormat.byteSeparator, bytesFormat.ignoreCase, "byte separator")
            }
            byteArray[byteIndex] = parseByteAt(charIndex + 1)
            charIndex += 3
        }
        return byteArray
    }
}

@ExperimentalStdlibApi
private fun String.hexToByteArrayNoLineAndGroupSeparatorSlowPath(
    startIndex: Int,
    endIndex: Int,
    bytesFormat: HexFormat.BytesHexFormat
): ByteArray? {
    val bytePrefix = bytesFormat.bytePrefix
    val byteSuffix = bytesFormat.byteSuffix
    val byteSeparator = bytesFormat.byteSeparator
    val byteSeparatorLength = byteSeparator.length
    val charsPerByte = 2L + bytePrefix.length + byteSuffix.length + byteSeparatorLength
    val numberOfChars = (endIndex - startIndex).toLong()
    val numberOfBytes = ((numberOfChars + byteSeparatorLength) / charsPerByte).toInt()

    // Go to the default implementation when the string length doesn't match
    if (numberOfBytes * charsPerByte - byteSeparatorLength != numberOfChars) {
        return null
    }

    val ignoreCase = bytesFormat.ignoreCase

    val byteArray = ByteArray(numberOfBytes)
    var charIndex = startIndex

    charIndex = checkContainsAt(charIndex, endIndex, bytePrefix, ignoreCase, "byte prefix")
    val between = byteSuffix + byteSeparator + bytePrefix
    for (byteIndex in 0 until numberOfBytes - 1) {
        byteArray[byteIndex] = parseByteAt(charIndex)
        charIndex = checkContainsAt(charIndex + 2, endIndex, between, ignoreCase, "byte suffix + byte separator + byte prefix")
    }
    byteArray[numberOfBytes - 1] = parseByteAt(charIndex)
    checkContainsAt(charIndex + 2, endIndex, byteSuffix, ignoreCase, "byte suffix")

    return byteArray
}

@ExperimentalStdlibApi
private fun String.hexToByteArraySlowPath(
    startIndex: Int,
    endIndex: Int,
    bytesFormat: HexFormat.BytesHexFormat
): ByteArray {
    val bytesPerLine = bytesFormat.bytesPerLine
    val bytesPerGroup = bytesFormat.bytesPerGroup
    val bytePrefix = bytesFormat.bytePrefix
    val byteSuffix = bytesFormat.byteSuffix
    val byteSeparator = bytesFormat.byteSeparator
    val groupSeparator = bytesFormat.groupSeparator
    val ignoreCase = bytesFormat.ignoreCase

    val parseMaxSize = parsedByteArrayMaxSize(
        stringLength = endIndex - startIndex,
        bytesPerLine,
        bytesPerGroup,
        groupSeparator.length,
        byteSeparator.length,
        bytePrefix.length,
        byteSuffix.length
    )
    val byteArray = ByteArray(parseMaxSize)

    var charIndex = startIndex
    var byteIndex = 0
    var indexInLine = 0
    var indexInGroup = 0

    while (charIndex < endIndex) {
        if (indexInLine == bytesPerLine) {
            charIndex = checkNewLineAt(charIndex, endIndex)
            indexInLine = 0
            indexInGroup = 0
        } else if (indexInGroup == bytesPerGroup) {
            charIndex = checkContainsAt(charIndex, endIndex, groupSeparator, ignoreCase, "group separator")
            indexInGroup = 0
        } else if (indexInGroup != 0) {
            charIndex = checkContainsAt(charIndex, endIndex, byteSeparator, ignoreCase, "byte separator")
        }
        indexInLine += 1
        indexInGroup += 1

        charIndex = checkContainsAt(charIndex, endIndex, bytePrefix, ignoreCase, "byte prefix")
        if (endIndex - 2 < charIndex) {
            throwInvalidNumberOfDigits(charIndex, endIndex, maxDigits = 2, requireMaxLength = true)
        }
        byteArray[byteIndex++] = parseByteAt(charIndex)
        charIndex = checkContainsAt(charIndex + 2, endIndex, byteSuffix, ignoreCase, "byte suffix")
    }

    return if (byteIndex == byteArray.size) byteArray else byteArray.copyOf(byteIndex)
}

private fun String.parseByteAt(index: Int): Byte {
    val high = decimalFromHexDigitAt(index)
    val low = decimalFromHexDigitAt(index + 1)
    return ((high shl 4) or low).toByte()
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
public fun Byte.toHexString(format: HexFormat = HexFormat.Default): String {
    val digits = if (format.upperCase) UPPER_CASE_HEX_DIGITS else LOWER_CASE_HEX_DIGITS
    val numberFormat = format.number

    // Optimize for digits-only formats
    if (numberFormat.isDigitsOnly) {
        val charArray = CharArray(2)
        val value = this.toInt()
        charArray[0] = digits[(value shr 4) and 0xF]
        charArray[1] = digits[value and 0xF]
        return if (numberFormat.removeLeadingZeros)
            charArray.concatToString(startIndex = (countLeadingZeroBits() shr 2).coerceAtMost(1))
        else
            charArray.concatToString()
    }

    return toLong().toHexStringImpl(numberFormat, digits, bits = 8)
}

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
    hexToIntImpl(startIndex, endIndex, format, maxDigits = 2).toByte()

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
public fun Short.toHexString(format: HexFormat = HexFormat.Default): String {
    val digits = if (format.upperCase) UPPER_CASE_HEX_DIGITS else LOWER_CASE_HEX_DIGITS
    val numberFormat = format.number

    // Optimize for digits-only formats
    if (numberFormat.isDigitsOnly) {
        val charArray = CharArray(4)
        val value = this.toInt()
        charArray[0] = digits[(value shr 12) and 0xF]
        charArray[1] = digits[(value shr 8) and 0xF]
        charArray[2] = digits[(value shr 4) and 0xF]
        charArray[3] = digits[value and 0xF]
        return if (numberFormat.removeLeadingZeros)
            charArray.concatToString(startIndex = (countLeadingZeroBits() shr 2).coerceAtMost(3))
        else
            charArray.concatToString()
    }

    return toLong().toHexStringImpl(numberFormat, digits, bits = 16)
}

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
    hexToIntImpl(startIndex, endIndex, format, maxDigits = 4).toShort()

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
public fun Int.toHexString(format: HexFormat = HexFormat.Default): String {
    val digits = if (format.upperCase) UPPER_CASE_HEX_DIGITS else LOWER_CASE_HEX_DIGITS
    val numberFormat = format.number

    // Optimize for digits-only formats
    if (numberFormat.isDigitsOnly) {
        val charArray = CharArray(8)
        val value = this
        charArray[0] = digits[(value shr 28) and 0xF]
        charArray[1] = digits[(value shr 24) and 0xF]
        charArray[2] = digits[(value shr 20) and 0xF]
        charArray[3] = digits[(value shr 16) and 0xF]
        charArray[4] = digits[(value shr 12) and 0xF]
        charArray[5] = digits[(value shr 8) and 0xF]
        charArray[6] = digits[(value shr 4) and 0xF]
        charArray[7] = digits[value and 0xF]
        return if (numberFormat.removeLeadingZeros)
            charArray.concatToString(startIndex = (countLeadingZeroBits() shr 2).coerceAtMost(7))
        else
            charArray.concatToString()
    }

    return toLong().toHexStringImpl(numberFormat, digits, bits = 32)
}

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
    hexToIntImpl(startIndex, endIndex, format, maxDigits = 8)

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
public fun Long.toHexString(format: HexFormat = HexFormat.Default): String {
    val digits = if (format.upperCase) UPPER_CASE_HEX_DIGITS else LOWER_CASE_HEX_DIGITS
    val numberFormat = format.number

    // Optimize for digits-only formats
    if (numberFormat.isDigitsOnly) {
        val charArray = CharArray(16)
        val value = this
        charArray[0] = digits[((value shr 60) and 0xF).toInt()]
        charArray[1] = digits[((value shr 56) and 0xF).toInt()]
        charArray[2] = digits[((value shr 52) and 0xF).toInt()]
        charArray[3] = digits[((value shr 48) and 0xF).toInt()]
        charArray[4] = digits[((value shr 44) and 0xF).toInt()]
        charArray[5] = digits[((value shr 40) and 0xF).toInt()]
        charArray[6] = digits[((value shr 36) and 0xF).toInt()]
        charArray[7] = digits[((value shr 32) and 0xF).toInt()]
        charArray[8] = digits[((value shr 28) and 0xF).toInt()]
        charArray[9] = digits[((value shr 24) and 0xF).toInt()]
        charArray[10] = digits[((value shr 20) and 0xF).toInt()]
        charArray[11] = digits[((value shr 16) and 0xF).toInt()]
        charArray[12] = digits[((value shr 12) and 0xF).toInt()]
        charArray[13] = digits[((value shr 8) and 0xF).toInt()]
        charArray[14] = digits[((value shr 4) and 0xF).toInt()]
        charArray[15] = digits[(value and 0xF).toInt()]
        return if (numberFormat.removeLeadingZeros)
            charArray.concatToString(startIndex = (countLeadingZeroBits() shr 2).coerceAtMost(15))
        else
            charArray.concatToString()
    }

    return toHexStringImpl(numberFormat, digits, bits = 64)
}

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
private fun Long.toHexStringImpl(numberFormat: HexFormat.NumberHexFormat, digits: String, bits: Int): String {
    require(bits and 0x3 == 0)

    val value = this
    val numberOfHexDigits = bits shr 2

    val prefix = numberFormat.prefix
    val suffix = numberFormat.suffix
    var removeZeros = numberFormat.removeLeadingZeros

    val formatLength = prefix.length.toLong() + numberOfHexDigits + suffix.length
    val charArray = CharArray(checkFormatLength(formatLength))

    var charIndex = prefix.toCharArrayIfNotEmpty(charArray, 0)

    var shift = bits
    repeat(numberOfHexDigits) {
        shift -= 4
        val decimal = ((value shr shift) and 0xF).toInt()
        removeZeros = removeZeros && decimal == 0 && shift > 0
        if (!removeZeros) {
            charArray[charIndex++] = digits[decimal]
        }
    }

    charIndex = suffix.toCharArrayIfNotEmpty(charArray, charIndex)

    return if (charIndex == charArray.size) charArray.concatToString() else charArray.concatToString(endIndex = charIndex)
}

@OptIn(ExperimentalStdlibApi::class)
private fun String.toCharArrayIfNotEmpty(destination: CharArray, destinationOffset: Int): Int {
    when (length) {
        0 -> { /* do nothing */ }
        1 -> destination[destinationOffset] = this[0]
        else -> toCharArray(destination, destinationOffset)
    }
    return destinationOffset + length
}

@ExperimentalStdlibApi
private fun String.hexToIntImpl(startIndex: Int, endIndex: Int, format: HexFormat, maxDigits: Int): Int {
    AbstractList.checkBoundsIndexes(startIndex, endIndex, length)

    val numberFormat = format.number

    // Optimize for digits-only formats
    if (numberFormat.isDigitsOnly) {
        checkMaxDigits(startIndex, endIndex, maxDigits)
        return parseInt(startIndex, endIndex)
    }

    val prefix = numberFormat.prefix
    val suffix = numberFormat.suffix
    checkPrefixSuffixMaxDigits(startIndex, endIndex, prefix, suffix, numberFormat.ignoreCase, maxDigits)
    return parseInt(startIndex + prefix.length, endIndex - suffix.length)
}

@ExperimentalStdlibApi
private fun String.hexToLongImpl(startIndex: Int, endIndex: Int, format: HexFormat, maxDigits: Int): Long {
    AbstractList.checkBoundsIndexes(startIndex, endIndex, length)

    val numberFormat = format.number

    // Optimize for digits-only formats
    if (numberFormat.isDigitsOnly) {
        checkMaxDigits(startIndex, endIndex, maxDigits)
        return parseLong(startIndex, endIndex)
    }

    val prefix = numberFormat.prefix
    val suffix = numberFormat.suffix
    checkPrefixSuffixMaxDigits(startIndex, endIndex, prefix, suffix, numberFormat.ignoreCase, maxDigits)
    return parseLong(startIndex + prefix.length, endIndex - suffix.length)
}

private fun String.checkPrefixSuffixMaxDigits(
    startIndex: Int,
    endIndex: Int,
    prefix: String,
    suffix: String,
    ignoreCase: Boolean,
    maxDigits: Int
) {
    if (endIndex - startIndex - prefix.length <= suffix.length) {
        throwInvalidPrefixSuffix(startIndex, endIndex, prefix, suffix)
    }

    val digitsStartIndex = checkContainsAt(startIndex, endIndex, prefix, ignoreCase, "prefix")
    val digitsEndIndex = endIndex - suffix.length
    checkContainsAt(digitsEndIndex, endIndex, suffix, ignoreCase, "suffix")

    checkMaxDigits(digitsStartIndex, digitsEndIndex, maxDigits)
}

private fun String.checkMaxDigits(startIndex: Int, endIndex: Int, maxDigits: Int) {
    if (startIndex >= endIndex || endIndex - startIndex > maxDigits) {
        throwInvalidNumberOfDigits(startIndex, endIndex, maxDigits, requireMaxLength = false)
    }
}

private fun String.parseInt(startIndex: Int, endIndex: Int): Int {
    var result = 0
    for (i in startIndex until endIndex) {
        result = (result shl 4) or decimalFromHexDigitAt(i)
    }
    return result
}

private fun String.parseLong(startIndex: Int, endIndex: Int): Long {
    var result = 0L
    for (i in startIndex until endIndex) {
        result = (result shl 4) or longDecimalFromHexDigitAt(i)
    }
    return result
}

@Suppress("NOTHING_TO_INLINE")
private inline fun String.checkContainsAt(index: Int, endIndex: Int, part: String, ignoreCase: Boolean, partName: String): Int {
    if (part.isEmpty()) return index
    for (i in part.indices) {
        if (!part[i].equals(this[index + i], ignoreCase)) {
            throwNotContainedAt(index, endIndex, part, partName)
        }
    }
    return index + part.length
}

@Suppress("NOTHING_TO_INLINE")
private inline fun String.decimalFromHexDigitAt(index: Int): Int {
    val code = this[index].code
    if (code ushr 8 == 0 && HEX_DIGITS_TO_DECIMAL[code] >= 0) {
        return HEX_DIGITS_TO_DECIMAL[code]
    }
    throwInvalidDigitAt(index)
}

@Suppress("NOTHING_TO_INLINE")
private inline fun String.longDecimalFromHexDigitAt(index: Int): Long {
    val code = this[index].code
    if (code ushr 8 == 0 && HEX_DIGITS_TO_LONG_DECIMAL[code] >= 0) {
        return HEX_DIGITS_TO_LONG_DECIMAL[code]
    }
    throwInvalidDigitAt(index)
}

private fun String.throwInvalidNumberOfDigits(startIndex: Int, endIndex: Int, maxDigits: Int, requireMaxLength: Boolean) {
    val specifier = if (requireMaxLength) "exactly" else "at most"
    val substring = substring(startIndex, endIndex)
    throw NumberFormatException(
        "Expected $specifier $maxDigits hexadecimal digits at index $startIndex, but was $substring of length ${endIndex - startIndex}"
    )
}

private fun String.throwNotContainedAt(index: Int, endIndex: Int, part: String, partName: String) {
    val substring = substring(index, (index + part.length).coerceAtMost(endIndex))
    throw NumberFormatException(
        "Expected $partName \"$part\" at index $index, but was $substring"
    )
}

private fun String.throwInvalidPrefixSuffix(startIndex: Int, endIndex: Int, prefix: String, suffix: String) {
    val substring = substring(startIndex, endIndex)
    throw NumberFormatException(
        "Expected a hexadecimal number with prefix \"$prefix\" and suffix \"$suffix\", but was $substring"
    )
}

private fun String.throwInvalidDigitAt(index: Int): Nothing {
    throw NumberFormatException("Expected a hexadecimal digit at index $index, but was ${this[index]}")
}
