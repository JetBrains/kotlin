/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.text.codec

/**
 * Provides encoding and decoding functionality for binary-to-text codecs.
 *
 * Only codecs with base equal to a power of two are supported.
 */
public open class BaseNCodec internal constructor(
    internal val encodeMap: ByteArray,
    internal val decodeMap: ByteArray
) {
    private val base get() = encodeMap.size

    private val bitsPerByte = Byte.SIZE_BITS
    internal val bitsPerSymbol = base.countTrailingZeroBits()

    private val bytesPerGroup: Int
    private val symbolsPerGroup: Int

    internal val paddingSymbol: Byte = 61 // '='

    init {
        require(base >= 2 && base and (base - 1) == 0) { "Only codecs with base equal to a power of two are supported." }
        require(bitsPerSymbol < bitsPerByte) { "Base is too big." }

        val lcm = lcm(bitsPerByte, bitsPerSymbol)
        bytesPerGroup = lcm / bitsPerByte
        symbolsPerGroup = lcm / bitsPerSymbol
    }

    private fun lcm(a: Int, b: Int): Int {
        require(a > b)
        for (k in 1..b) {
            val multiple = k * a
            if (multiple % b == 0) {
                return multiple
            }
        }
        error("Unreachable")
    }

    private fun encodeSize(source: ByteArray): Int {
        // includes padding chars
        return ((source.size + bytesPerGroup - 1) / bytesPerGroup) * symbolsPerGroup
    }

    private fun symbolAt(index: Int): Byte = encodeMap[index]

    /**
     * Encodes bytes from the specified [source] array.
     */
    public fun encode(source: ByteArray): ByteArray {
        val result = ByteArray(encodeSize(source))
        var resultIndex = 0

        var payload = 0
        var symbolStart = -bitsPerSymbol
        for (byte in source) {
            payload = (payload shl bitsPerByte) or byte.toInt()
            symbolStart += bitsPerByte

            while (symbolStart >= 0) {
                result[resultIndex++] = symbolAt(payload ushr symbolStart)

                payload = payload and ((1 shl symbolStart) - 1)
                symbolStart -= bitsPerSymbol
            }
        }

        if (symbolStart == -bitsPerSymbol) {
            check(payload == 0)
            check(resultIndex == result.size)
            return result
        }

        result[resultIndex++] = symbolAt(payload shl -symbolStart)

        while (resultIndex < result.size) {
            result[resultIndex++] = paddingSymbol
        }

        return result
    }

    /**
     * Encodes bytes from the specified [source] array and returns a string with the resulting symbols.
     */
    public fun encodeToString(source: ByteArray): String {
        val byteResult = encode(source)
        return buildString(byteResult.size) {
            for (byte in byteResult) {
                append(byte.toInt().toChar())
            }
        }
    }

    /**
     * Encodes bytes from the specified [source] array or its subrange and writes encoded symbols into the [destination] array.
     * Returns the number of symbols written.
     *
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
    public fun encode(
        source: ByteArray,
        destination: ByteArray,
        destinationOffset: Int = 0,
        startIndex: Int = 0,
        endIndex: Int = source.size
    ): Int {
        // TODO: move the encoding algorithm to this function
        AbstractList.checkBoundsIndexes(startIndex, endIndex, source.size)
        val result = encode(source)
        AbstractList.checkBoundsIndexes(destinationOffset, destinationOffset + result.size, destination.size)
        result.copyInto(destination, destinationOffset)
        return result.size
    }

    private fun paddingsCount(source: ByteArray): Int {
        var paddings = 0
        while (paddings < source.size && source[source.lastIndex - paddings] == paddingSymbol) {
            paddings++
        }
        return paddings
    }

    private fun decodeSize(sourceSize: Int, paddings: Int): Int {
        return ((sourceSize - paddings) * bitsPerSymbol) / bitsPerByte
    }

    private fun throwIllegalSymbol(symbol: Int, index: Int): Nothing {
        throw throw IllegalArgumentException("Invalid symbol '${symbol.toChar()}'(${symbol.toString(radix = 8)}) at index $index")
    }

    private fun decodeSymbol(symbol: Int, index: Int): Int {
        val bits = decodeMap[symbol]
        if (bits < 0) {
            throwIllegalSymbol(symbol, index)
        }
        return bits.toInt()
    }

    /**
     * Decodes symbols from the specified [source] array.
     */
    public fun decode(source: ByteArray): ByteArray {
        val paddings = paddingsCount(source)
        val result = ByteArray(decodeSize(source.size, paddings))
        var resultIndex = 0

        var payload = 0
        var byteStart = -bitsPerByte

        for (sourceIndex in 0 until source.size - paddings) {
            val symbol = source[sourceIndex].toInt() and 0xFF

            payload = (payload shl bitsPerSymbol) or decodeSymbol(symbol, sourceIndex)
            byteStart += bitsPerSymbol

            if (byteStart >= 0) {
                result[resultIndex++] = (payload ushr byteStart).toByte()

                payload = payload and ((1 shl byteStart) - 1)
                byteStart -= bitsPerByte
            }
        }

        check(payload == 0)
//        check(byteStart == -bitsPerByte)
        check(resultIndex == result.size)

        return result
    }

    /**
     * Decodes symbols from the specified [source] string.
     */
    public fun decodeFromString(source: String): ByteArray {
        val byteSource = ByteArray(source.length) {
            val symbol = source[it].code
            if (symbol > Byte.MAX_VALUE) {
                throwIllegalSymbol(symbol, it)
            }
            symbol.toByte()
        }

        return decode(byteSource)
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
    public fun decode(
        source: ByteArray,
        destination: ByteArray,
        destinationOffset: Int = 0,
        startIndex: Int = 0,
        endIndex: Int = source.size
    ): Int {
        // TODO: move the decoding algorithm to this function
        AbstractList.checkBoundsIndexes(startIndex, endIndex, source.size)
        val result = decode(source)
        AbstractList.checkBoundsIndexes(destinationOffset, destinationOffset + result.size, destination.size)
        result.copyInto(destination, destinationOffset)
        return result.size
    }

    private fun isInAlphabet(value: Int): Boolean = value in decodeMap.indices
            && value != paddingSymbol.toInt()
            && decodeMap[value] > 0

    /**
     * Returns `true` if the given [value] is a valid symbol in this codec.
     */
    public fun isInAlphabet(value: Byte): Boolean = isInAlphabet(value.toInt())

    /**
     * Returns `true` if the given [value] is a valid symbol in this codec.
     */
    public fun isInAlphabet(value: Char): Boolean = isInAlphabet(value.code)
}
