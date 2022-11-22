/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.io.encoding

/**
 * The "base64" encoding specified by [RFC 4648 section 4](https://www.rfc-editor.org/rfc/rfc4648#section-4), Base 64 Encoding.
 *
 * The character `'='` is used for padding.
 */
public open class Base64 private constructor(
    private val encodeMap: ByteArray,
    private val decodeMap: ByteArray,
    // TODO: Take this parameter into consideration.
    // https://www.rfc-editor.org/rfc/rfc4648#section-3.3
    //  It skips invalid symbols (including line separators) when decoding, and inserts line separators when encoding.
    //  Discuss line length and line separator string.
    // The encoded output stream must be represented in lines of no more
    //   than 76 characters each.  All line breaks or other characters not
    //   found in Table 1 must be ignored by decoding software.
    @Suppress("unused") private val isMimeScheme: Boolean
) {
    init {
        require(encodeMap.size == 64)
    }

    private fun encodeSize(sourceSize: Int): Int {
        // includes padding chars
        // TODO: Int overflow
        return ((sourceSize + bytesPerGroup - 1) / bytesPerGroup) * symbolsPerGroup
    }

    private fun symbolAt(index: Int): Byte = encodeMap[index]

    /**
     * Encodes bytes from the specified [source] array.
     */
    public fun encodeToByteArray(source: ByteArray): ByteArray {
        val result = ByteArray(encodeSize(source.size))
        encodeToByteArray(source, result)
        return result
    }

    /**
     * Encodes bytes from the specified [source] array or its subrange and writes encoded symbols into the [destination] array.
     * Returns the number of symbols written.
     *
     * @param source the array to encode bytes from.
     * @param destination the array to write symbols into.
     * @param destinationOffset the starting index in the [destination] array to write symbols to, 0 by default.
     * @param startIndex the beginning (inclusive) of the subrange to encode, 0 by default.
     * @param endIndex the end (exclusive) of the subrange to encode, size of the [source] array by default.
     *
     * @throws IndexOutOfBoundsException when [startIndex] or [endIndex] is out of range of [source] array indices.
     * @throws IllegalArgumentException when `startIndex > endIndex`.
     * @throws IndexOutOfBoundsException when the resulting symbols don't fit into the [destination] array starting at the specified [destinationOffset],
     * or when that index is out of the [destination] array indices range.
     *
     * @return the number of symbols written into [destination] array.
     */
    public fun encodeToByteArray(
        source: ByteArray,
        destination: ByteArray,
        destinationOffset: Int = 0,
        startIndex: Int = 0,
        endIndex: Int = source.size
    ): Int {
        AbstractList.checkBoundsIndexes(startIndex, endIndex, source.size)
        val encodeSize = encodeSize(endIndex - startIndex)
        AbstractList.checkBoundsIndexes(destinationOffset, destinationOffset + encodeSize, destination.size)

        var destinationIndex = destinationOffset

        var payload = 0
        var symbolStart = -bitsPerSymbol
        for (byte in source) {
            payload = (payload shl bitsPerByte) or (byte.toInt() and 0xFF)
            symbolStart += bitsPerByte

            while (symbolStart >= 0) {
                destination[destinationIndex++] = symbolAt(payload ushr symbolStart)

                payload = payload and ((1 shl symbolStart) - 1)
                symbolStart -= bitsPerSymbol
            }
        }

        if (symbolStart == -bitsPerSymbol) {
            check(payload == 0)
            check(destinationIndex == destinationOffset + encodeSize)
            return encodeSize
        }

        destination[destinationIndex++] = symbolAt(payload shl -symbolStart)

        while (destinationIndex < encodeSize) {
            destination[destinationIndex++] = paddingSymbol
        }

        return encodeSize
    }

    /**
     * Encodes bytes from the specified [source] array and returns a string with the resulting symbols.
     */
    public fun encode(source: ByteArray): String {
        val byteResult = encodeToByteArray(source)
        return buildString(byteResult.size) {
            for (byte in byteResult) {
                append(byte.toInt().toChar())
            }
        }
    }

    /**
     * Encodes bytes from the specified [source] array or its subrange and appends encoded symbols to the [destination] appendable.
     * Returns the number of symbols appended.
     *
     * @param source the array to encode bytes from.
     * @param destination the appendable to append symbols to.
     * @param startIndex the beginning (inclusive) of the subrange to encode, 0 by default.
     * @param endIndex the end (exclusive) of the subrange to encode, size of the [source] array by default.
     *
     * @throws IndexOutOfBoundsException when [startIndex] or [endIndex] is out of range of [source] array indices.
     * @throws IllegalArgumentException when `startIndex > endIndex`.
     *
     * @return the number of symbols appended to the [destination] appendable.
     */
    public fun encode(
        source: ByteArray,
        destination: Appendable,
        startIndex: Int = 0,
        endIndex: Int = source.size
    ): Int {
        val byteResult = ByteArray(encodeSize(endIndex - startIndex))
        val symbolsAppended = encodeToByteArray(source, byteResult, 0, startIndex, endIndex)
        for (byte in byteResult) {
            destination.append(byte.toInt().toChar())
        }
        return symbolsAppended
    }

    private fun paddingsCount(source: ByteArray, startIndex: Int, endIndex: Int): Int {
        var paddingIndex = endIndex - 1
        while (paddingIndex >= startIndex && source[paddingIndex] == paddingSymbol) {
            paddingIndex--
        }
        return endIndex - paddingIndex - 1
    }

    private fun decodeSize(sourceSize: Int, paddings: Int): Int {
        // TODO: Int overflow
        return ((sourceSize - paddings) * bitsPerSymbol) / bitsPerByte
    }

    private fun throwIllegalSymbol(symbol: Int, index: Int): Nothing {
        throw throw IllegalArgumentException("Invalid symbol '${symbol.toChar()}'(${symbol.toString(radix = 8)}) at index $index")
    }

    private fun decodeSymbol(symbol: Int, index: Int): Int {
        if (symbol >= decodeMap.size || decodeMap[symbol] < 0) {
            throwIllegalSymbol(symbol, index)
        }
        return decodeMap[symbol].toInt()
    }

    /**
     * Decodes symbols from the specified [source] array.
     */
    public fun decodeFromByteArray(source: ByteArray): ByteArray {
        val paddings = paddingsCount(source, 0, source.size)
        val decodeSize = decodeSize(source.size, paddings)
        val result = ByteArray(decodeSize)
        decodeFromByteArray(source, result)
        return result
    }

    /**
     * Decodes symbols from the specified [source] array or its subrange and writes decoded bytes into the [destination] array.
     * Returns the number of bytes written.
     *
     * @param destination the array to write bytes into.
     * @param destinationOffset the starting index in the [destination] array to write bytes to, 0 by default.
     * @param startIndex the beginning (inclusive) of the subrange to decode, 0 by default.
     * @param endIndex the end (exclusive) of the subrange to decode, size of the [source] array by default.
     *
     * @throws IndexOutOfBoundsException when [startIndex] or [endIndex] is out of range of [source] array indices.
     * @throws IllegalArgumentException when `startIndex > endIndex`.
     * @throws IndexOutOfBoundsException when the resulting bytes don't fit into the [destination] array starting at the specified [destinationOffset],
     * or when that index is out of the [destination] array indices range.
     *
     * @return the number of bytes written into [destination] array.
     */
    public fun decodeFromByteArray(
        source: ByteArray,
        destination: ByteArray,
        destinationOffset: Int = 0,
        startIndex: Int = 0,
        endIndex: Int = source.size
    ): Int {
        AbstractList.checkBoundsIndexes(startIndex, endIndex, source.size)
        val paddings = paddingsCount(source, startIndex, endIndex)
        val decodeSize = decodeSize(sourceSize = endIndex - startIndex, paddings)
        AbstractList.checkBoundsIndexes(destinationOffset, destinationOffset + decodeSize, destination.size)

        var destinationIndex = destinationOffset

        var payload = 0
        var byteStart = -bitsPerByte

        for (sourceIndex in startIndex until endIndex - paddings) {
            val symbol = source[sourceIndex].toInt() and 0xFF
            val symbolBits = decodeSymbol(symbol, sourceIndex)
            if (symbolBits < 0) {
                continue
            }

            payload = (payload shl bitsPerSymbol) or symbolBits
            byteStart += bitsPerSymbol

            if (byteStart >= 0) {
                destination[destinationIndex++] = (payload ushr byteStart).toByte()

                payload = payload and ((1 shl byteStart) - 1)
                byteStart -= bitsPerByte
            }
        }

//        check(payload == 0) // the padded bits are zeros
        check(destinationIndex == destinationOffset + decodeSize)

        return decodeSize
    }

    /**
     * Decodes symbols from the specified [source] string.
     */
    public fun decode(source: String): ByteArray {
        val byteSource = ByteArray(source.length) {
            val symbol = source[it].code
            if (symbol > Byte.MAX_VALUE) {
                throwIllegalSymbol(symbol, it)
            }
            symbol.toByte()
        }

        return decodeFromByteArray(byteSource)
    }

    private fun isInAlphabet(value: Int): Boolean = value in decodeMap.indices
            && value != paddingSymbol.toInt()
            && decodeMap[value] > 0

    /**
     * Returns `true` if the given [value] is a valid symbol in this Base64.
     */
    public fun isInAlphabet(value: Byte): Boolean = isInAlphabet(value.toInt())

    /**
     * Returns `true` if the given [value] is a valid symbol in this Base64.
     */
    public fun isInAlphabet(value: Char): Boolean = isInAlphabet(value.code)

    public companion object Default : Base64(base64EncodeMap, base64DecodeMap, isMimeScheme = false) {

        private const val bitsPerByte = 8
        private const val bitsPerSymbol = 6

        private const val bytesPerGroup: Int = 3
        private const val symbolsPerGroup: Int = 4

        private const val paddingSymbol: Byte = 61 // '='

        public val UrlSafe: Base64
            get() = Base64(base64UrlEncodeMap, base64UrlDecodeMap, isMimeScheme = false)

        public val Mime: Base64
            get() = Base64(base64EncodeMap, base64DecodeMap, isMimeScheme = true)

        @Suppress("UNUSED_PARAMETER")
        public fun Mime(lineLength: Int, lineSeparator: String): Base64 = TODO()
    }
}


// "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/"
private val base64EncodeMap = byteArrayOf(
    65,  66,  67,  68,  69,  70,  71,  72,  73,  74,  75,  76,  77,  78,  79,  80,  /* 0 - 15 */
    81,  82,  83,  84,  85,  86,  87,  88,  89,  90,  97,  98,  99,  100, 101, 102, /* 16 - 31 */
    103, 104, 105, 106, 107, 108, 109, 110, 111, 112, 113, 114, 115, 116, 117, 118, /* 32 - 47 */
    119, 120, 121, 122, 48,  49,  50,  51,  52,  53,  54,  55,  56,  57,  43,  47,  /* 48 - 63 */
)

private val base64DecodeMap = byteArrayOf(
    -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, /* 0 - 15 */
    -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, /* 16 - 31 */
    -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, 62, -1, -1, -1, 63, /* 32 - 47 */
    52, 53, 54, 55, 56, 57, 58, 59, 60, 61, -1, -1, -1, -1, -1, -1, /* 48 - 63 */
    -1,  0,  1,  2,  3,  4,  5,  6,  7,  8,  9, 10, 11, 12, 13, 14, /* 64 - 79 */
    15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, -1, -1, -1, -1, -1, /* 80 - 95 */
    -1, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35, 36, 37, 38, 39, 40, /* 96 - 111 */
    41, 42, 43, 44, 45, 46, 47, 48, 49, 50, 51, -1, -1, -1, -1, -1, /* 112 - 127 */
)

// "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-_"
private val base64UrlEncodeMap = byteArrayOf(
    65,  66,  67,  68,  69,  70,  71,  72,  73,  74,  75,  76,  77,  78,  79,  80,  /* 0 - 15 */
    81,  82,  83,  84,  85,  86,  87,  88,  89,  90,  97,  98,  99,  100, 101, 102, /* 16 - 31 */
    103, 104, 105, 106, 107, 108, 109, 110, 111, 112, 113, 114, 115, 116, 117, 118, /* 32 - 47 */
    119, 120, 121, 122, 48,  49,  50,  51,  52,  53,  54,  55,  56,  57,  45,  95,  /* 48 - 63 */
)

private val base64UrlDecodeMap = byteArrayOf(
    -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, /* 0 - 15 */
    -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, /* 16 - 31 */
    -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, 62, -1, -1, /* 32 - 47 */
    52, 53, 54, 55, 56, 57, 58, 59, 60, 61, -1, -1, -1, -1, -1, -1, /* 48 - 63 */
    -1,  0,  1,  2,  3,  4,  5,  6,  7,  8,  9, 10, 11, 12, 13, 14, /* 64 - 79 */
    15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, -1, -1, -1, -1, 63, /* 80 - 95 */
    -1, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35, 36, 37, 38, 39, 40, /* 96 - 111 */
    41, 42, 43, 44, 45, 46, 47, 48, 49, 50, 51, -1, -1, -1, -1, -1, /* 112 - 127 */
)