/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.text

/** Returns the negative [size] if [throwOnMalformed] is false, throws [CharacterCodingException] otherwise. */
private fun malformed(size: Int, index: Int, throwOnMalformed: Boolean): Int {
    if (throwOnMalformed) throw CharacterCodingException("Malformed sequence starting at ${index - 1}")
    return -size
}

/**
 * Returns code point corresponding to UTF-16 surrogate pair,
 * where the first of the pair is the [high] and the second is in the [string] at the [index].
 * Returns zero if the pair is malformed and [throwOnMalformed] is false.
 *
 * @throws CharacterCodingException if the pair is malformed and [throwOnMalformed] is true.
 */
private fun codePointFromSurrogate(string: String, high: Int, index: Int, endIndex: Int, throwOnMalformed: Boolean): Int {
    if (high !in 0xD800..0xDBFF || index >= endIndex) {
        return malformed(0, index, throwOnMalformed)
    }
    val low = string[index].code
    if (low !in 0xDC00..0xDFFF) {
        return malformed(0, index, throwOnMalformed)
    }
    return 0x10000 + ((high and 0x3FF) shl 10) or (low and 0x3FF)
}

/**
 * Returns code point corresponding to UTF-8 sequence of two bytes,
 * where the first byte of the sequence is the [byte1] and the second byte is in the [bytes] array at the [index].
 * Returns zero if the sequence is malformed and [throwOnMalformed] is false.
 *
 * @throws CharacterCodingException if the sequence of two bytes is malformed and [throwOnMalformed] is true.
 */
private fun codePointFrom2(bytes: ByteArray, byte1: Int, index: Int, endIndex: Int, throwOnMalformed: Boolean): Int {
    if (byte1 and 0x1E == 0 || index >= endIndex) {
        return malformed(0, index, throwOnMalformed)
    }
    val byte2 = bytes[index].toInt()
    if (byte2 and 0xC0 != 0x80) {
        return malformed(0, index, throwOnMalformed)
    }
    return (byte1 shl 6) xor byte2 xor 0xF80
}

/**
 * Returns code point corresponding to UTF-8 sequence of three bytes,
 * where the first byte of the sequence is the [byte1] and the others are in the [bytes] array starting from the [index].
 * Returns a non-positive value indicating number of bytes from [bytes] included in malformed sequence
 * if the sequence is malformed and [throwOnMalformed] is false.
 *
 * @throws CharacterCodingException if the sequence of three bytes is malformed and [throwOnMalformed] is true.
 */
private fun codePointFrom3(bytes: ByteArray, byte1: Int, index: Int, endIndex: Int, throwOnMalformed: Boolean): Int {
    if (index >= endIndex) {
        return malformed(0, index, throwOnMalformed)
    }

    val byte2 = bytes[index].toInt()
    if (byte1 and 0xF == 0) {
        if (byte2 and 0xE0 != 0xA0) {
            // Non-shortest form
            return malformed(0, index, throwOnMalformed)
        }
    } else if (byte1 and 0xF == 0xD) {
        if (byte2 and 0xE0 != 0x80) {
            // Surrogate code point
            return malformed(0, index, throwOnMalformed)
        }
    } else if (byte2 and 0xC0 != 0x80) {
        return malformed(0, index, throwOnMalformed)
    }

    if (index + 1 == endIndex) {
        return malformed(1, index, throwOnMalformed)
    }
    val byte3 = bytes[index + 1].toInt()
    if (byte3 and 0xC0 != 0x80) {
        return malformed(1, index, throwOnMalformed)
    }

    return (byte1 shl 12) xor (byte2 shl 6) xor byte3 xor -0x1E080
}

/**
 * Returns code point corresponding to UTF-8 sequence of four bytes,
 * where the first byte of the sequence is the [byte1] and the others are in the [bytes] array starting from the [index].
 * Returns a non-positive value indicating number of bytes from [bytes] included in malformed sequence
 * if the sequence is malformed and [throwOnMalformed] is false.
 *
 * @throws CharacterCodingException if the sequence of four bytes is malformed and [throwOnMalformed] is true.
 */
private fun codePointFrom4(bytes: ByteArray, byte1: Int, index: Int, endIndex: Int, throwOnMalformed: Boolean): Int {
    if (index >= endIndex) {
        return malformed(0, index, throwOnMalformed)
    }

    val byte2 = bytes[index].toInt()
    if (byte1 and 0xF == 0x0) {
        if (byte2 and 0xF0 <= 0x80) {
            // Non-shortest form
            return malformed(0, index, throwOnMalformed)
        }
    } else if (byte1 and 0xF == 0x4) {
        if (byte2 and 0xF0 != 0x80) {
            // Out of Unicode code points domain (larger than U+10FFFF)
            return malformed(0, index, throwOnMalformed)
        }
    } else if (byte1 and 0xF > 0x4) {
        return malformed(0, index, throwOnMalformed)
    }

    if (byte2 and 0xC0 != 0x80) {
        return malformed(0, index, throwOnMalformed)
    }

    if (index + 1 == endIndex) {
        return malformed(1, index, throwOnMalformed)
    }
    val byte3 = bytes[index + 1].toInt()
    if (byte3 and 0xC0 != 0x80) {
        return malformed(1, index, throwOnMalformed)
    }

    if (index + 2 == endIndex) {
        return malformed(2, index, throwOnMalformed)
    }
    val byte4 = bytes[index + 2].toInt()
    if (byte4 and 0xC0 != 0x80) {
        return malformed(2, index, throwOnMalformed)
    }
    return (byte1 shl 18) xor (byte2 shl 12) xor (byte3 shl 6) xor byte4 xor 0x381F80
}

/**
 * Maximum number of bytes needed to encode a single char.
 *
 * Code points in `0..0x7F` are encoded in a single byte.
 * Code points in `0x80..0x7FF` are encoded in two bytes.
 * Code points in `0x800..0xD7FF` or in `0xE000..0xFFFF` are encoded in three bytes.
 * Surrogate code points in `0xD800..0xDFFF` are not Unicode scalar values, therefore aren't encoded.
 * Code points in `0x10000..0x10FFFF` are represented by a pair of surrogate `Char`s and are encoded in four bytes.
 */
private const val MAX_BYTES_PER_CHAR = 3

/**
 * The byte sequence a malformed UTF-16 char sequence is replaced by.
 */
private val REPLACEMENT_BYTE_SEQUENCE: ByteArray = byteArrayOf(0xEF.toByte(), 0xBF.toByte(), 0xBD.toByte())

/**
 * Encodes the [string] using UTF-8 and returns the resulting [ByteArray].
 *
 * @param string the string to encode.
 * @param startIndex the start offset (inclusive) of the substring to encode.
 * @param endIndex the end offset (exclusive) of the substring to encode.
 * @param throwOnMalformed whether to throw on malformed char sequence or replace by the [REPLACEMENT_BYTE_SEQUENCE].
 *
 * @throws CharacterCodingException if the char sequence is malformed and [throwOnMalformed] is true.
 */
internal fun encodeUtf8(string: String, startIndex: Int, endIndex: Int, throwOnMalformed: Boolean): ByteArray {
    require(startIndex >= 0 && endIndex <= string.length && startIndex <= endIndex)

    val bytes = ByteArray((endIndex - startIndex) * MAX_BYTES_PER_CHAR)
    var byteIndex = 0
    var charIndex = startIndex

    while (charIndex < endIndex) {
        val code = string[charIndex++].code
        when {
            code < 0x80 ->
                bytes[byteIndex++] = code.toByte()
            code < 0x800 -> {
                bytes[byteIndex++] = ((code shr 6) or 0xC0).toByte()
                bytes[byteIndex++] = ((code and 0x3F) or 0x80).toByte()
            }
            code < 0xD800 || code >= 0xE000 -> {
                bytes[byteIndex++] = ((code shr 12) or 0xE0).toByte()
                bytes[byteIndex++] = (((code shr 6) and 0x3F) or 0x80).toByte()
                bytes[byteIndex++] = ((code and 0x3F) or 0x80).toByte()
            }
            else -> { // Surrogate char value
                val codePoint = codePointFromSurrogate(string, code, charIndex, endIndex, throwOnMalformed)
                if (codePoint <= 0) {
                    bytes[byteIndex++] = REPLACEMENT_BYTE_SEQUENCE[0]
                    bytes[byteIndex++] = REPLACEMENT_BYTE_SEQUENCE[1]
                    bytes[byteIndex++] = REPLACEMENT_BYTE_SEQUENCE[2]
                } else {
                    bytes[byteIndex++] = ((codePoint shr 18) or 0xF0).toByte()
                    bytes[byteIndex++] = (((codePoint shr 12) and 0x3F) or 0x80).toByte()
                    bytes[byteIndex++] = (((codePoint shr 6) and 0x3F) or 0x80).toByte()
                    bytes[byteIndex++] = ((codePoint and 0x3F) or 0x80).toByte()
                    charIndex++
                }
            }
        }
    }

    return if (bytes.size == byteIndex) bytes else bytes.copyOf(byteIndex)
}

/**
 * The character a malformed UTF-8 byte sequence is replaced by.
 */
private const val REPLACEMENT_CHAR = '\uFFFD'

/**
 * Decodes the UTF-8 [bytes] array and returns the resulting [String].
 *
 * @param bytes the byte array to decode.
 * @param startIndex the start offset (inclusive) of the array to be decoded.
 * @param endIndex the end offset (exclusive) of the array to be encoded.
 * @param throwOnMalformed whether to throw on malformed byte sequence or replace by the [REPLACEMENT_CHAR].
 *
 * @throws CharacterCodingException if the array is malformed UTF-8 byte sequence and [throwOnMalformed] is true.
 */
internal fun decodeUtf8(bytes: ByteArray, startIndex: Int, endIndex: Int, throwOnMalformed: Boolean): String {
    require(startIndex >= 0 && endIndex <= bytes.size && startIndex <= endIndex)

    var byteIndex = startIndex
    val stringBuilder = StringBuilder()

    while (byteIndex < endIndex) {
        val byte = bytes[byteIndex++].toInt()
        when {
            byte >= 0 ->
                stringBuilder.append(byte.toChar())
            byte shr 5 == -2 -> {
                val code = codePointFrom2(bytes, byte, byteIndex, endIndex, throwOnMalformed)
                if (code <= 0) {
                    stringBuilder.append(REPLACEMENT_CHAR)
                    byteIndex += -code
                } else {
                    stringBuilder.append(code.toChar())
                    byteIndex += 1
                }
            }
            byte shr 4 == -2 -> {
                val code = codePointFrom3(bytes, byte, byteIndex, endIndex, throwOnMalformed)
                if (code <= 0) {
                    stringBuilder.append(REPLACEMENT_CHAR)
                    byteIndex += -code
                } else {
                    stringBuilder.append(code.toChar())
                    byteIndex += 2
                }
            }
            byte shr 3 == -2 -> {
                val code = codePointFrom4(bytes, byte, byteIndex, endIndex, throwOnMalformed)
                if (code <= 0) {
                    stringBuilder.append(REPLACEMENT_CHAR)
                    byteIndex += -code
                } else {
                    val high = (code - 0x10000) shr 10 or 0xD800
                    val low = (code and 0x3FF) or 0xDC00
                    stringBuilder.append(high.toChar())
                    stringBuilder.append(low.toChar())
                    byteIndex += 3
                }
            }
            else -> {
                val _ = malformed(0, byteIndex, throwOnMalformed)
                stringBuilder.append(REPLACEMENT_CHAR)
            }
        }
    }

    return stringBuilder.toString()
}
