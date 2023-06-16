/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.io.encoding

// Benchmarks repository: https://github.com/qurbonzoda/KotlinBase64Benchmarks

/**
 * Provides Base64 encoding and decoding functionality.
 *
 * This class is not supposed to be instantiated or inherited.
 * However, predefined instances of this class are available for use.
 * The companion object [Base64.Default] is the default instance of [Base64].
 * There are also [Base64.UrlSafe] and [Base64.Mime] instances.
 */
@SinceKotlin("1.8")
@ExperimentalEncodingApi
public open class Base64 private constructor(
    internal val isUrlSafe: Boolean,
    internal val isMimeScheme: Boolean
) {
    init {
        require(!isUrlSafe || !isMimeScheme)
    }

    /**
     * Encodes bytes from the specified [source] array or its subrange.
     * Returns a [ByteArray] containing the resulting symbols.
     *
     * If the size of the [source] array or its subrange is not an integral multiple of 3,
     * the result is padded with `'='` to an integral multiple of 4 symbols.
     *
     * Each resulting symbol occupies one byte in the returned byte array.
     *
     * Use [encode] to get the output in string form.
     *
     * @param source the array to encode bytes from.
     * @param startIndex the beginning (inclusive) of the subrange to encode, 0 by default.
     * @param endIndex the end (exclusive) of the subrange to encode, size of the [source] array by default.
     *
     * @throws IndexOutOfBoundsException when [startIndex] or [endIndex] is out of range of [source] array indices.
     * @throws IllegalArgumentException when `startIndex > endIndex`.
     *
     * @return a [ByteArray] with the resulting symbols.
     */
    public fun encodeToByteArray(source: ByteArray, startIndex: Int = 0, endIndex: Int = source.size): ByteArray {
        return platformEncodeToByteArray(source, startIndex, endIndex)
    }

    /**
     * Encodes bytes from the specified [source] array or its subrange and writes resulting symbols into the [destination] array.
     * Returns the number of symbols written.
     *
     * If the size of the [source] array or its subrange is not an integral multiple of 3,
     * the result is padded with `'='` to an integral multiple of 4 symbols.
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
    public fun encodeIntoByteArray(
        source: ByteArray,
        destination: ByteArray,
        destinationOffset: Int = 0,
        startIndex: Int = 0,
        endIndex: Int = source.size
    ): Int {
        return platformEncodeIntoByteArray(source, destination, destinationOffset, startIndex, endIndex)
    }

    /**
     * Encodes bytes from the specified [source] array or its subrange.
     * Returns a string with the resulting symbols.
     *
     * If the size of the [source] array or its subrange is not an integral multiple of 3,
     * the result is padded with `'='` to an integral multiple of 4 symbols.
     *
     * Use [encodeToByteArray] to get the output in [ByteArray] form.
     *
     * @param source the array to encode bytes from.
     * @param startIndex the beginning (inclusive) of the subrange to encode, 0 by default.
     * @param endIndex the end (exclusive) of the subrange to encode, size of the [source] array by default.
     *
     * @throws IndexOutOfBoundsException when [startIndex] or [endIndex] is out of range of [source] array indices.
     * @throws IllegalArgumentException when `startIndex > endIndex`.
     *
     * @return a string with the resulting symbols.
     */
    public fun encode(source: ByteArray, startIndex: Int = 0, endIndex: Int = source.size): String {
        return platformEncodeToString(source, startIndex, endIndex)
    }

    /**
     * Encodes bytes from the specified [source] array or its subrange and appends resulting symbols to the [destination] appendable.
     * Returns the destination appendable.
     *
     * If the size of the [source] array or its subrange is not an integral multiple of 3,
     * the result is padded with `'='` to an integral multiple of 4 symbols.
     *
     * @param source the array to encode bytes from.
     * @param destination the appendable to append symbols to.
     * @param startIndex the beginning (inclusive) of the subrange to encode, 0 by default.
     * @param endIndex the end (exclusive) of the subrange to encode, size of the [source] array by default.
     *
     * @throws IndexOutOfBoundsException when [startIndex] or [endIndex] is out of range of [source] array indices.
     * @throws IllegalArgumentException when `startIndex > endIndex`.
     *
     * @return the destination appendable.
     */
    public fun <A : Appendable> encodeToAppendable(
        source: ByteArray,
        destination: A,
        startIndex: Int = 0,
        endIndex: Int = source.size
    ): A {
        val stringResult = platformEncodeToString(source, startIndex, endIndex)
        destination.append(stringResult)
        return destination
    }

    /**
     * Decodes symbols from the specified [source] array or its subrange.
     * Returns a [ByteArray] containing the resulting bytes.
     *
     * The symbols for decoding are not required to be padded.
     * However, if there is a padding character present, the correct amount of padding character(s) must be present.
     * The padding character `'='` is interpreted as the end of the encoded byte data. Subsequent symbols are prohibited.
     *
     * @param source the array to decode symbols from.
     * @param startIndex the beginning (inclusive) of the subrange to decode, 0 by default.
     * @param endIndex the end (exclusive) of the subrange to decode, size of the [source] array by default.
     *
     * @throws IndexOutOfBoundsException when [startIndex] or [endIndex] is out of range of [source] array indices.
     * @throws IllegalArgumentException when `startIndex > endIndex`.
     * @throws IllegalArgumentException when the symbols for decoding are padded incorrectly or there are extra symbols after the padding.
     *
     * @return a [ByteArray] with the resulting bytes.
     */
    public fun decode(source: ByteArray, startIndex: Int = 0, endIndex: Int = source.size): ByteArray {
        checkSourceBounds(source.size, startIndex, endIndex)

        val decodeSize = decodeSize(source, startIndex, endIndex)
        val destination = ByteArray(decodeSize)

        val bytesWritten = decodeImpl(source, destination, 0, startIndex, endIndex)

        check(bytesWritten == destination.size)

        return destination
    }

    /**
     * Decodes symbols from the specified [source] array or its subrange and writes resulting bytes into the [destination] array.
     * Returns the number of bytes written.
     *
     * The symbols for decoding are not required to be padded.
     * However, if there is a padding character present, the correct amount of padding character(s) must be present.
     * The padding character `'='` is interpreted as the end of the encoded byte data. Subsequent symbols are prohibited.
     *
     * @param source the array to decode symbols from.
     * @param destination the array to write bytes into.
     * @param destinationOffset the starting index in the [destination] array to write bytes to, 0 by default.
     * @param startIndex the beginning (inclusive) of the subrange to decode, 0 by default.
     * @param endIndex the end (exclusive) of the subrange to decode, size of the [source] array by default.
     *
     * @throws IndexOutOfBoundsException when [startIndex] or [endIndex] is out of range of [source] array indices.
     * @throws IllegalArgumentException when `startIndex > endIndex`.
     * @throws IndexOutOfBoundsException when the resulting bytes don't fit into the [destination] array starting at the specified [destinationOffset],
     * or when that index is out of the [destination] array indices range.
     * @throws IllegalArgumentException when the symbols for decoding are padded incorrectly or there are extra symbols after the padding.
     *
     * @return the number of bytes written into [destination] array.
     */
    public fun decodeIntoByteArray(
        source: ByteArray,
        destination: ByteArray,
        destinationOffset: Int = 0,
        startIndex: Int = 0,
        endIndex: Int = source.size
    ): Int {
        checkSourceBounds(source.size, startIndex, endIndex)
        checkDestinationBounds(destination.size, destinationOffset, decodeSize(source, startIndex, endIndex))

        return decodeImpl(source, destination, destinationOffset, startIndex, endIndex)
    }

    /**
     * Decodes symbols from the specified [source] char sequence or its substring.
     * Returns a [ByteArray] containing the resulting bytes.
     *
     * The symbols for decoding are not required to be padded.
     * However, if there is a padding character present, the correct amount of padding character(s) must be present.
     * The padding character `'='` is interpreted as the end of the encoded byte data. Subsequent symbols are prohibited.
     *
     * @param source the char sequence to decode symbols from.
     * @param startIndex the beginning (inclusive) of the substring to decode, 0 by default.
     * @param endIndex the end (exclusive) of the substring to decode, length of the [source] by default.
     *
     * @throws IndexOutOfBoundsException when [startIndex] or [endIndex] is out of range of [source] indices.
     * @throws IllegalArgumentException when `startIndex > endIndex`.
     * @throws IllegalArgumentException when the symbols for decoding are padded incorrectly or there are extra symbols after the padding.
     *
     * @return a [ByteArray] with the resulting bytes.
     */
    public fun decode(source: CharSequence, startIndex: Int = 0, endIndex: Int = source.length): ByteArray {
        val byteSource = platformCharsToBytes(source, startIndex, endIndex)
        return decode(byteSource)
    }

    /**
     * Decodes symbols from the specified [source] char sequence or its substring and writes resulting bytes into the [destination] array.
     * Returns the number of bytes written.
     *
     * The symbols for decoding are not required to be padded.
     * However, if there is a padding character present, the correct amount of padding character(s) must be present.
     * The padding character `'='` is interpreted as the end of the encoded byte data. Subsequent symbols are prohibited.
     *
     * @param source the char sequence to decode symbols from.
     * @param destination the array to write bytes into.
     * @param destinationOffset the starting index in the [destination] array to write bytes to, 0 by default.
     * @param startIndex the beginning (inclusive) of the substring to decode, 0 by default.
     * @param endIndex the end (exclusive) of the substring to decode, length of the [source] by default.
     *
     * @throws IndexOutOfBoundsException when [startIndex] or [endIndex] is out of range of [source] indices.
     * @throws IllegalArgumentException when `startIndex > endIndex`.
     * @throws IndexOutOfBoundsException when the resulting bytes don't fit into the [destination] array starting at the specified [destinationOffset],
     * or when that index is out of the [destination] array indices range.
     * @throws IllegalArgumentException when the symbols for decoding are padded incorrectly or there are extra symbols after the padding.
     *
     * @return the number of bytes written into [destination] array.
     */
    public fun decodeIntoByteArray(
        source: CharSequence,
        destination: ByteArray,
        destinationOffset: Int = 0,
        startIndex: Int = 0,
        endIndex: Int = source.length
    ): Int {
        val byteSource = platformCharsToBytes(source, startIndex, endIndex)
        return decodeIntoByteArray(byteSource, destination, destinationOffset)
    }

    // internal functions

    internal fun encodeToByteArrayImpl(source: ByteArray, startIndex: Int, endIndex: Int): ByteArray {
        checkSourceBounds(source.size, startIndex, endIndex)

        val encodeSize = encodeSize(endIndex - startIndex)
        val destination = ByteArray(encodeSize)
        encodeIntoByteArrayImpl(source, destination, 0, startIndex, endIndex)
        return destination
    }

    internal fun encodeIntoByteArrayImpl(
        source: ByteArray,
        destination: ByteArray,
        destinationOffset: Int,
        startIndex: Int,
        endIndex: Int
    ): Int {
        checkSourceBounds(source.size, startIndex, endIndex)
        checkDestinationBounds(destination.size, destinationOffset, encodeSize(endIndex - startIndex))

        val encodeMap = if (isUrlSafe) base64UrlEncodeMap else base64EncodeMap
        var sourceIndex = startIndex
        var destinationIndex = destinationOffset
        val groupsPerLine = if (isMimeScheme) mimeGroupsPerLine else Int.MAX_VALUE

        while (sourceIndex + 2 < endIndex) {
            val groups = minOf((endIndex - sourceIndex) / bytesPerGroup, groupsPerLine)
            for (i in 0 until groups) {
                val byte1 = source[sourceIndex++].toInt() and 0xFF
                val byte2 = source[sourceIndex++].toInt() and 0xFF
                val byte3 = source[sourceIndex++].toInt() and 0xFF
                val bits = (byte1 shl 16) or (byte2 shl 8) or byte3
                destination[destinationIndex++] = encodeMap[bits ushr 18]
                destination[destinationIndex++] = encodeMap[(bits ushr 12) and 0x3F]
                destination[destinationIndex++] = encodeMap[(bits ushr 6) and 0x3F]
                destination[destinationIndex++] = encodeMap[bits and 0x3F]
            }
            if (groups == groupsPerLine && sourceIndex != endIndex) {
                destination[destinationIndex++] = mimeLineSeparatorSymbols[0]
                destination[destinationIndex++] = mimeLineSeparatorSymbols[1]
            }
        }

        when (endIndex - sourceIndex) {
            1 -> {
                val byte1 = source[sourceIndex++].toInt() and 0xFF
                val bits = byte1 shl 4
                destination[destinationIndex++] = encodeMap[bits ushr 6]
                destination[destinationIndex++] = encodeMap[bits and 0x3F]
                destination[destinationIndex++] = padSymbol
                destination[destinationIndex++] = padSymbol
            }
            2 -> {
                val byte1 = source[sourceIndex++].toInt() and 0xFF
                val byte2 = source[sourceIndex++].toInt() and 0xFF
                val bits = (byte1 shl 10) or (byte2 shl 2)
                destination[destinationIndex++] = encodeMap[bits ushr 12]
                destination[destinationIndex++] = encodeMap[(bits ushr 6) and 0x3F]
                destination[destinationIndex++] = encodeMap[bits and 0x3F]
                destination[destinationIndex++] = padSymbol
            }
        }

        check(sourceIndex == endIndex)

        return destinationIndex - destinationOffset
    }

    private fun encodeSize(sourceSize: Int): Int {
        // includes padding chars
        val groups = (sourceSize + bytesPerGroup - 1) / bytesPerGroup
        val lineSeparators = if (isMimeScheme) (groups - 1) / mimeGroupsPerLine else 0
        val size = groups * symbolsPerGroup + lineSeparators * 2
        if (size < 0) { // Int overflow
            throw IllegalArgumentException("Input is too big")
        }
        return size
    }

    private fun decodeImpl(
        source: ByteArray,
        destination: ByteArray,
        destinationOffset: Int,
        startIndex: Int,
        endIndex: Int
    ): Int {
        val decodeMap = if (isUrlSafe) base64UrlDecodeMap else base64DecodeMap
        var payload = 0
        var byteStart = -bitsPerByte
        var sourceIndex = startIndex
        var destinationIndex = destinationOffset

        while (sourceIndex < endIndex) {
            if (byteStart == -bitsPerByte && sourceIndex + 3 < endIndex) {
                val symbol1 = decodeMap[source[sourceIndex++].toInt() and 0xFF]
                val symbol2 = decodeMap[source[sourceIndex++].toInt() and 0xFF]
                val symbol3 = decodeMap[source[sourceIndex++].toInt() and 0xFF]
                val symbol4 = decodeMap[source[sourceIndex++].toInt() and 0xFF]
                val bits = (symbol1 shl 18) or (symbol2 shl 12) or (symbol3 shl 6) or symbol4
                if (bits >= 0) { // all base64 symbols
                    destination[destinationIndex++] = (bits shr 16).toByte()
                    destination[destinationIndex++] = (bits shr 8).toByte()
                    destination[destinationIndex++] = bits.toByte()
                    continue
                }
                sourceIndex -= 4
            }

            val symbol = source[sourceIndex].toInt() and 0xFF
            val symbolBits = decodeMap[symbol]
            if (symbolBits < 0) {
                if (symbolBits == -2) {
                    sourceIndex = handlePaddingSymbol(source, sourceIndex, endIndex, byteStart)
                    break
                } else if (isMimeScheme) {
                    sourceIndex += 1
                    continue
                } else {
                    throw IllegalArgumentException("Invalid symbol '${symbol.toChar()}'(${symbol.toString(radix = 8)}) at index $sourceIndex")
                }
            } else {
                sourceIndex += 1
            }

            payload = (payload shl bitsPerSymbol) or symbolBits
            byteStart += bitsPerSymbol

            if (byteStart >= 0) {
                destination[destinationIndex++] = (payload ushr byteStart).toByte()

                payload = payload and ((1 shl byteStart) - 1)
                byteStart -= bitsPerByte
            }
        }

        // pad or end of input

        if (byteStart == -bitsPerByte + bitsPerSymbol) { // dangling single symbol, incorrectly encoded
            throw IllegalArgumentException("The last unit of input does not have enough bits")
        }

//        check(payload == 0) // the padded bits are allowed to be non-zero

        sourceIndex = skipIllegalSymbolsIfMime(source, sourceIndex, endIndex)
        if (sourceIndex < endIndex) {
            val symbol = source[sourceIndex].toInt() and 0xFF
            throw IllegalArgumentException("Symbol '${symbol.toChar()}'(${symbol.toString(radix = 8)}) at index ${sourceIndex - 1} is prohibited after the pad character")
        }

        return destinationIndex - destinationOffset
    }

    private fun decodeSize(source: ByteArray, startIndex: Int, endIndex: Int): Int {
        var symbols = endIndex - startIndex
        if (symbols == 0) {
            return 0
        }
        if (symbols == 1) {
            throw IllegalArgumentException("Input should have at list 2 symbols for Base64 decoding, startIndex: $startIndex, endIndex: $endIndex")
        }
        if (isMimeScheme) {
            for (index in startIndex until endIndex) {
                val symbol = source[index].toInt() and 0xFF
                val symbolBits = base64DecodeMap[symbol]
                if (symbolBits < 0) {
                    if (symbolBits == -2) {
                        symbols -= endIndex - index
                        break
                    }
                    symbols--
                }
            }
        } else if (source[endIndex - 1] == padSymbol) {
            symbols--
            if (source[endIndex - 2] == padSymbol) {
                symbols--
            }
        }
        return ((symbols.toLong() * bitsPerSymbol) / bitsPerByte).toInt() // conversion due to possible Int overflow
    }

    internal fun charsToBytesImpl(source: CharSequence, startIndex: Int, endIndex: Int): ByteArray {
        checkSourceBounds(source.length, startIndex, endIndex)

        val byteArray = ByteArray(endIndex - startIndex)
        var length = 0
        for (index in startIndex until endIndex) {
            val symbol = source[index].code
            if (symbol <= 0xFF) {
                byteArray[length++] = symbol.toByte()
            } else {
                // the replacement byte must be an illegal symbol
                // so that mime skips it and basic throws with correct index
                byteArray[length++] = 0x3F
            }
        }
        return byteArray
    }

    internal fun bytesToStringImpl(source: ByteArray): String {
        val stringBuilder = StringBuilder(source.size)
        for (byte in source) {
            stringBuilder.append(byte.toInt().toChar())
        }
        return stringBuilder.toString()
    }

    private fun handlePaddingSymbol(source: ByteArray, padIndex: Int, endIndex: Int, byteStart: Int): Int {
        return when (byteStart) {
            -bitsPerByte -> // =
                throw IllegalArgumentException("Redundant pad character at index $padIndex")
            -bitsPerByte + bitsPerSymbol -> // x=, dangling single symbol
                padIndex + 1
            -bitsPerByte + 2 * bitsPerSymbol - bitsPerByte -> { // xx=
                val secondPadIndex = skipIllegalSymbolsIfMime(source, padIndex + 1, endIndex)
                if (secondPadIndex == endIndex || source[secondPadIndex] != padSymbol) {
                    throw IllegalArgumentException("Missing one pad character at index $secondPadIndex")
                }
                secondPadIndex + 1
            }
            -bitsPerByte + 3 * bitsPerSymbol - 2 * bitsPerByte -> // xxx=
                padIndex + 1
            else ->
                error("Unreachable")
        }
    }

    private fun skipIllegalSymbolsIfMime(source: ByteArray, startIndex: Int, endIndex: Int): Int {
        if (!isMimeScheme) {
            return startIndex
        }
        var sourceIndex = startIndex
        while (sourceIndex < endIndex) {
            val symbol = source[sourceIndex].toInt() and 0xFF
            if (base64DecodeMap[symbol] != -1) {
                return sourceIndex
            }
            sourceIndex += 1
        }
        return sourceIndex
    }

    internal fun checkSourceBounds(sourceSize: Int, startIndex: Int, endIndex: Int) {
        AbstractList.checkBoundsIndexes(startIndex, endIndex, sourceSize)
    }

    private fun checkDestinationBounds(destinationSize: Int, destinationOffset: Int, capacityNeeded: Int) {
        if (destinationOffset < 0 || destinationOffset > destinationSize) {
            throw IndexOutOfBoundsException("destination offset: $destinationOffset, destination size: $destinationSize")
        }

        val destinationEndIndex = destinationOffset + capacityNeeded
        if (destinationEndIndex < 0 || destinationEndIndex > destinationSize) {
            throw IndexOutOfBoundsException(
                "The destination array does not have enough capacity, " +
                        "destination offset: $destinationOffset, destination size: $destinationSize, capacity needed: $capacityNeeded"
            )
        }
    }

    // companion object

    /**
     * The "base64" encoding specified by [`RFC 4648 section 4`](https://www.rfc-editor.org/rfc/rfc4648#section-4),
     * Base 64 Encoding.
     *
     * Uses "The Base 64 Alphabet" as specified in Table 1 of RFC 4648 for encoding and decoding.
     * Encode operation does not add any line separator character.
     * Decode operation throws if it encounters a character outside the base64 alphabet.
     *
     * The character `'='` is used for padding.
     */
    public companion object Default : Base64(isUrlSafe = false, isMimeScheme = false) {

        private const val bitsPerByte: Int = 8
        private const val bitsPerSymbol: Int = 6

        internal const val bytesPerGroup: Int = 3
        internal const val symbolsPerGroup: Int = 4

        internal const val padSymbol: Byte = 61 // '='

        internal const val mimeLineLength: Int = 76
        private const val mimeGroupsPerLine: Int = mimeLineLength / symbolsPerGroup
        internal val mimeLineSeparatorSymbols: ByteArray = byteArrayOf('\r'.code.toByte(), '\n'.code.toByte())

        /**
         * The "base64url" encoding specified by [`RFC 4648 section 5`](https://www.rfc-editor.org/rfc/rfc4648#section-5),
         * Base 64 Encoding with URL and Filename Safe Alphabet.
         *
         * Uses "The URL and Filename safe Base 64 Alphabet" as specified in Table 2 of RFC 4648 for encoding and decoding.
         * Encode operation does not add any line separator character.
         * Decode operation throws if it encounters a character outside the base64url alphabet.
         *
         * The character `'='` is used for padding.
         */
        public val UrlSafe: Base64 = Base64(isUrlSafe = true, isMimeScheme = false)

        /**
         * The encoding specified by [`RFC 2045 section 6.8`](https://www.rfc-editor.org/rfc/rfc2045#section-6.8),
         * Base64 Content-Transfer-Encoding.
         *
         * Uses "The Base64 Alphabet" as specified in Table 1 of RFC 2045 for encoding and decoding.
         * Encode operation adds CRLF every 76 symbols. No line separator is added to the end of the encoded output.
         * Decode operation ignores all line separators and other characters outside the base64 alphabet.
         *
         * The character `'='` is used for padding.
         */
        public val Mime: Base64 = Base64(isUrlSafe = false, isMimeScheme = true)
    }
}


// "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/"
private val base64EncodeMap = byteArrayOf(
    65,  66,  67,  68,  69,  70,  71,  72,  73,  74,  75,  76,  77,  78,  79,  80,  /* 0 - 15 */
    81,  82,  83,  84,  85,  86,  87,  88,  89,  90,  97,  98,  99,  100, 101, 102, /* 16 - 31 */
    103, 104, 105, 106, 107, 108, 109, 110, 111, 112, 113, 114, 115, 116, 117, 118, /* 32 - 47 */
    119, 120, 121, 122, 48,  49,  50,  51,  52,  53,  54,  55,  56,  57,  43,  47,  /* 48 - 63 */
)

@ExperimentalEncodingApi
private val base64DecodeMap = IntArray(256).apply {
    this.fill(-1)
    this[Base64.padSymbol.toInt()] = -2
    base64EncodeMap.forEachIndexed { index, symbol ->
        this[symbol.toInt()] = index
    }
}

// "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-_"
private val base64UrlEncodeMap = byteArrayOf(
    65,  66,  67,  68,  69,  70,  71,  72,  73,  74,  75,  76,  77,  78,  79,  80,  /* 0 - 15 */
    81,  82,  83,  84,  85,  86,  87,  88,  89,  90,  97,  98,  99,  100, 101, 102, /* 16 - 31 */
    103, 104, 105, 106, 107, 108, 109, 110, 111, 112, 113, 114, 115, 116, 117, 118, /* 32 - 47 */
    119, 120, 121, 122, 48,  49,  50,  51,  52,  53,  54,  55,  56,  57,  45,  95,  /* 48 - 63 */
)

@ExperimentalEncodingApi
private val base64UrlDecodeMap = IntArray(256).apply {
    this.fill(-1)
    this[Base64.padSymbol.toInt()] = -2
    base64UrlEncodeMap.forEachIndexed { index, symbol ->
        this[symbol.toInt()] = index
    }
}


@SinceKotlin("1.8")
@ExperimentalEncodingApi
internal fun isInMimeAlphabet(symbol: Int): Boolean {
    return symbol in base64DecodeMap.indices && base64DecodeMap[symbol] != -1
}


@SinceKotlin("1.8")
@ExperimentalEncodingApi
internal expect fun Base64.platformCharsToBytes(
    source: CharSequence,
    startIndex: Int,
    endIndex: Int
): ByteArray


@SinceKotlin("1.8")
@ExperimentalEncodingApi
internal expect fun Base64.platformEncodeToString(
    source: ByteArray,
    startIndex: Int,
    endIndex: Int
): String

@SinceKotlin("1.8")
@ExperimentalEncodingApi
internal expect fun Base64.platformEncodeIntoByteArray(
    source: ByteArray,
    destination: ByteArray,
    destinationOffset: Int,
    startIndex: Int,
    endIndex: Int
): Int

@SinceKotlin("1.8")
@ExperimentalEncodingApi
internal expect fun Base64.platformEncodeToByteArray(
    source: ByteArray,
    startIndex: Int,
    endIndex: Int
): ByteArray