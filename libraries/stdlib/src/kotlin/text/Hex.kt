/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.text

// similar to joinToString
// prefix and suffix are added after each formatted byte
public open class Hex(
    private val separator: CharSequence = ", ", // rename separator -> delimiter ?
    private val prefix: CharSequence = "",
    private val suffix: CharSequence = "",
    private val upperCase: Boolean = false
) {
    private val formatLengthPerByte: Int
        get() = prefix.length + 2 + suffix.length

    private fun formatLength(bytes: Int): Int {
        // TODO: handle Int overflow
        return (formatLengthPerByte + separator.length) * bytes - separator.length
    }

    private fun formatDigit(digit: Int): Char =
        if (upperCase) upperCaseFormatMap[digit] else lowerCaseFormatMap[digit]

    // rename array -> source, bytes ?
    public fun format(array: ByteArray, startIndex: Int = 0, endIndex: Int = array.size): String {
        AbstractList.checkBoundsIndexes(startIndex, endIndex, array.size)

        val length = formatLength(endIndex - startIndex)
        return buildString(length) { format(array, this, startIndex, endIndex) }
    }

    public fun format(
        array: ByteArray,
        destination: Appendable,
        startIndex: Int = 0,
        endIndex: Int = array.size
    ): Int {
        AbstractList.checkBoundsIndexes(startIndex, endIndex, array.size)

        destination.apply {
            for (index in startIndex until endIndex) {
                val byte = array[index].toInt() and 0xFF
                append(prefix)
                append(formatDigit(byte shr 4))
                append(formatDigit(byte and 0xF))
                append(suffix)
                if (index < endIndex - 1) {
                    append(separator)
                }
            }
        }

        return formatLength(endIndex - startIndex)
    }

    private fun parseLength(chars: Int): Int {
        // TODO: handle Int overflow
        return (chars + separator.length) / (formatLengthPerByte + separator.length)
    }

    private fun checkStartsWith(
        charSequence: CharSequence,
        index: Int,
        endIndex: Int,
        substring: CharSequence,
        substringDescription: String
    ) {
        if (index + substring.length > endIndex || !charSequence.startsWith(substring, index)) {
            throw IllegalArgumentException("Expected $substringDescription: <$substring> at index: <$index> until endIndex: <$endIndex>")
        }
    }

    private fun parseByte(charSequence: CharSequence, index: Int, endIndex: Int): Byte {
        if (index + 2 > endIndex) {
            return -1
        }
        val first = charSequence[index].code
        val second = charSequence[index + 1].code
        if (first >= parseMap.size || second >= parseMap.size || parseMap[first] == -1 || parseMap[second] == -1) {
            return -1
        }
        return ((parseMap[first] shl 4) or parseMap[second]).toByte()
    }

    // rename charSequence -> input, value
    public fun parse(charSequence: CharSequence, startIndex: Int = 0, endIndex: Int = charSequence.length): ByteArray {
        AbstractList.checkBoundsIndexes(startIndex, endIndex, charSequence.length)

        val result = ByteArray(parseLength(endIndex - startIndex))
        parse(charSequence, result)
        return result
    }

    // both upper and lower case characters are accepted, and can even be mixed
    public fun parse(
        charSequence: CharSequence,
        destination: ByteArray,
        destinationOffset: Int = 0,
        startIndex: Int = 0,
        endIndex: Int = charSequence.length
    ): Int {
        AbstractList.checkBoundsIndexes(startIndex, endIndex, charSequence.length)
        val parseLength = parseLength(endIndex - startIndex)
        AbstractList.checkBoundsIndexes(destinationOffset, destinationOffset + parseLength, destination.size)

        var destinationIndex = destinationOffset
        var index = startIndex
        while (index < endIndex) {
            checkStartsWith(charSequence, index, endIndex, prefix, "prefix")
            index += prefix.length

            val byte = parseByte(charSequence, index, endIndex)
            if (byte < 0) {
                throw IllegalArgumentException("Expected 2 hex digits at index: <$index> until endIndex: <$endIndex>")
            }
            destination[destinationIndex++] = byte
            index += 2

            checkStartsWith(charSequence, index, endIndex, suffix, "suffix")
            index += suffix.length

            if (index < endIndex) {
                checkStartsWith(charSequence, index, endIndex, separator, "separator")
                index += separator.length
            }
        }

        check(index == endIndex)
        check(destinationIndex - destinationOffset == parseLength)

        return destinationIndex - destinationOffset
    }

    companion object Default : Hex(separator = "") {
        private const val lowerCaseFormatMap = "0123456789abcdef"
        private const val upperCaseFormatMap = "0123456789ABCDEF"

        private val parseMap = IntArray(128) { -1 }.apply {
            lowerCaseFormatMap.forEachIndexed { index, char -> this[char.code] = index }
            upperCaseFormatMap.forEachIndexed { index, char -> this[char.code] = index }
        }
    }
}


// HexFormat.toHexDigits(Primitive): String
public fun Int.toHexDigits(): String = toUInt().toString(radix = 16).padStart(8, '0')
public fun Int.toHexDigitsWithBytes(): String = Hex.format(this.bytes())

private fun Int.bytes(): ByteArray = byteArrayOf(
    (this ushr 24).toByte(),
    (this ushr 16).toByte(),
    (this ushr 8).toByte(),
    this.toByte()
)

// HexFormat.fromHexDigits(String): Primitive
public fun String.hexDigitsToInt(): Int {
    return this.toUInt(radix = 16).toInt()
}
