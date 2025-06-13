/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.io.encoding

// Benchmarks repository: https://github.com/qurbonzoda/KotlinBase64Benchmarks

/**
 * Provides Base64 encoding and decoding functionality.
 * Base64 encoding, as defined by the [`RFC 4648`](https://www.rfc-editor.org/rfc/rfc4648) and a few other RFCs,
 * transforms arbitrary binary data into a sequence of printable characters.
 *
 * For example, a sequence of bytes `0xC0 0xFF 0xEE` will be transformed to a string `"wP/u"` using a Base64 encoding
 * defined by the [`RFC 4648`](https://www.rfc-editor.org/rfc/rfc4648). Decoding that string will result in the original
 * byte sequence.
 *
 * **Base64 is not an encryption scheme and should not be used when data needs to be secured or obfuscated.**
 *
 * Characters used by a particular Base64 scheme form an alphabet of 64 regular characters and
 * an extra padding character.
 *
 * Almost all Base64 encoding schemes share the same first 62 characters of an alphabet,
 * which are `'A'..'Z'` followed by `'a'..'z'`, but the last two characters may vary.
 * [`RFC 4648 section 4`](https://www.rfc-editor.org/rfc/rfc4648#section-4) defines an alphabet that uses
 * `'+'` and `'/'` as the last two characters, while the URL-safe alphabet defined in
 * [`RFC 4648 section 5`](https://www.rfc-editor.org/rfc/rfc4648#section-5) uses `'-'` and `'_'` instead.
 *
 * When decoding, a Base64 scheme usually accepts only characters from its alphabet, presence of any other characters
 * is treated as an error (see [Base64.Mime] for an exception to this rule). That also implies
 * that a Base64 scheme could not decode data encoded by another Base64 scheme if their alphabets differ.
 *
 * In addition to 64 characters from the alphabet,
 * Base64-encoded data may also contain one or two padding characters `'='` at its end.
 * Base64 splits data that needs to be encoded into chunks three bytes long, which are then transformed into
 * a sequence of four characters (meaning that every character encodes six bits of data).
 * If the number of bytes constituting input data is not a multiple of three (for instance, input consists of only five bytes),
 * the data will be padded by zero bits first and only then transformed into Base64-alphabet characters.
 * If padding takes place, the resulting string is augmented by `'='`. The padding could consist of zero, two or
 * four bits, thus encoded data will contain zero, one or two padding characters (`'='`), respectively.
 * The inclusion of padding characters in the resulting string depends on the [PaddingOption] set for the Base64 instance.
 *
 * This class is not supposed to be inherited or instantiated by calling its constructor.
 * However, predefined instances of this class are available for use.
 * The companion object [Base64.Default] is the default instance of [Base64].
 * There are also [Base64.UrlSafe] and [Base64.Mime] instances.
 * The padding option for all predefined instances is set to [PaddingOption.PRESENT].
 * New instances with different padding options can be created using the [withPadding] function.
 *
 * @sample samples.io.encoding.Base64Samples.encodeAndDecode
 * @sample samples.io.encoding.Base64Samples.encodingDifferences
 * @sample samples.io.encoding.Base64Samples.padding
 */
@SinceKotlin("2.2")
@WasExperimental(ExperimentalEncodingApi::class)
public open class Base64 private constructor(
    internal val isUrlSafe: Boolean,
    internal val isMimeScheme: Boolean,
    internal val mimeLineLength: Int,
    internal val paddingOption: PaddingOption
) {
    init {
        require(!isUrlSafe || !isMimeScheme)
    }

    private val mimeGroupsPerLine: Int = mimeLineLength / symbolsPerGroup


    /**
     * An enumeration of the possible padding options for Base64 encoding and decoding.
     *
     * Constants of this enum class can be passed to the [withPadding] function to create a new Base64 instance
     * with the specified padding option. Each padding option affects the encode and decode operations of the
     * Base64 instance in the following way:
     *
     * | PaddingOption                    | On encode    | On decode                |
     * |----------------------------------|--------------|--------------------------|
     * | [PaddingOption.PRESENT]          | Emit padding | Padding is required      |
     * | [PaddingOption.ABSENT]           | Omit padding | Padding must not present |
     * | [PaddingOption.PRESENT_OPTIONAL] | Emit padding | Padding is optional      |
     * | [PaddingOption.ABSENT_OPTIONAL]  | Omit padding | Padding is optional      |
     *
     * These options provide flexibility in handling the padding characters (`'='`) and enable compatibility with
     * various Base64 libraries and protocols.
     *
     * @sample samples.io.encoding.Base64Samples.paddingOptionSample
     */
    @SinceKotlin("2.0")
    public enum class PaddingOption {
        /**
         * Pad on encode, require padding on decode.
         *
         * When encoding, the result is padded with `'='` to reach an integral multiple of 4 symbols.
         * When decoding, correctly padded input is required. The padding character `'='` marks the end
         * of the encoded data, and subsequent symbols are prohibited.
         *
         * This represents the canonical form of Base64 encoding.
         *
         * @sample samples.io.encoding.Base64Samples.paddingOptionPresentSample
         */
        PRESENT,

        /**
         * Do not pad on encode, prohibit padding on decode.
         *
         * When encoding, the result is not padded.
         * When decoding, the input must not contain any padding character.
         *
         * @sample samples.io.encoding.Base64Samples.paddingOptionAbsentSample
         */
        ABSENT,

        /**
         * Pad on encode, allow optional padding on decode.
         *
         * When encoding, the result is padded with `'='` to reach an integral multiple of 4 symbols.
         * When decoding, the input may be either padded or unpadded. If the input contains a padding character,
         * the correct amount of padding character(s) must be present. The padding character `'='`
         * marks the end of the encoded data, and subsequent symbols are prohibited.
         *
         * @sample samples.io.encoding.Base64Samples.paddingOptionPresentOptionalSample
         */
        PRESENT_OPTIONAL,

        /**
         * Do not pad on encode, allow optional padding on decode.
         *
         * When encoding, the result is not padded.
         * When decoding, the input may be either padded or unpadded. If the input contains a padding character,
         * the correct amount of padding character(s) must be present. The padding character `'='`
         * marks the end of the encoded data, and subsequent symbols are prohibited.
         *
         * @sample samples.io.encoding.Base64Samples.paddingOptionAbsentOptionalSample
         */
        ABSENT_OPTIONAL
    }

    /**
     * Returns a new [Base64] instance that is equivalent to this instance
     * but configured with the specified padding [option].
     *
     * This method does not modify this instance. If the specified [option] is the same as the current
     * padding option of this instance, this instance itself is returned. Otherwise, a new instance is created
     * using the same alphabet but configured with the new padding option.
     *
     * @sample samples.io.encoding.Base64Samples.withPaddingSample
     */
    @SinceKotlin("2.0")
    public fun withPadding(option: PaddingOption): Base64 {
        return if (paddingOption == option) this
        else Base64(isUrlSafe, isMimeScheme, mimeLineLength, option)
    }

    /**
     * Encodes bytes from the specified [source] array or its subrange.
     * Returns a [ByteArray] containing the resulting symbols.
     *
     * Whether the encoding result is padded with `'='` depends on the [PaddingOption] set for this [Base64] instance.
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
     *
     * @sample samples.io.encoding.Base64Samples.encodeToByteArraySample
     */
    public fun encodeToByteArray(source: ByteArray, startIndex: Int = 0, endIndex: Int = source.size): ByteArray {
        return platformEncodeToByteArray(source, startIndex, endIndex)
    }

    /**
     * Encodes bytes from the specified [source] array or its subrange and writes resulting symbols into the [destination] array.
     * Returns the number of symbols written.
     *
     * Whether the encoding result is padded with `'='` depends on the [PaddingOption] set for this [Base64] instance.
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
     *
     * @sample samples.io.encoding.Base64Samples.encodeIntoByteArraySample
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
     * Whether the encoding result is padded with `'='` depends on the [PaddingOption] set for this [Base64] instance.
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
     *
     * @sample samples.io.encoding.Base64Samples.encodeToStringSample
     */
    public fun encode(source: ByteArray, startIndex: Int = 0, endIndex: Int = source.size): String {
        return platformEncodeToString(source, startIndex, endIndex)
    }

    /**
     * Encodes bytes from the specified [source] array or its subrange and appends resulting symbols to the [destination] appendable.
     * Returns the destination appendable.
     *
     * Whether the encoding result is padded with `'='` depends on the [PaddingOption] set for this [Base64] instance.
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
     *
     * @sample samples.io.encoding.Base64Samples.encodeToAppendableSample
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
     * The requirement, prohibition, or optionality of padding in the input symbols
     * is determined by the [PaddingOption] set for this [Base64] instance.
     *
     * @param source the array to decode symbols from.
     * @param startIndex the beginning (inclusive) of the subrange to decode, 0 by default.
     * @param endIndex the end (exclusive) of the subrange to decode, size of the [source] array by default.
     *
     * @throws IndexOutOfBoundsException when [startIndex] or [endIndex] is out of range of [source] array indices.
     * @throws IllegalArgumentException when `startIndex > endIndex`.
     * @throws IllegalArgumentException when the symbols for decoding are not padded as required by the [PaddingOption]
     *   set for this [Base64] instance, or when there are extra symbols after the padding.
     *
     * @return a [ByteArray] with the resulting bytes.
     *
     * @sample samples.io.encoding.Base64Samples.decodeFromByteArraySample
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
     * The requirement, prohibition, or optionality of padding in the input symbols
     * is determined by the [PaddingOption] set for this [Base64] instance.
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
     * @throws IllegalArgumentException when the symbols for decoding are not padded as required by the [PaddingOption]
     *   set for this [Base64] instance, or when there are extra symbols after the padding.
     *
     * @return the number of bytes written into [destination] array.
     *
     * @sample samples.io.encoding.Base64Samples.decodeIntoByteArrayFromByteArraySample
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
     * The requirement, prohibition, or optionality of padding in the input symbols
     * is determined by the [PaddingOption] set for this [Base64] instance.
     *
     * @param source the char sequence to decode symbols from.
     * @param startIndex the beginning (inclusive) of the substring to decode, 0 by default.
     * @param endIndex the end (exclusive) of the substring to decode, length of the [source] by default.
     *
     * @throws IndexOutOfBoundsException when [startIndex] or [endIndex] is out of range of [source] indices.
     * @throws IllegalArgumentException when `startIndex > endIndex`.
     * @throws IllegalArgumentException when the symbols for decoding are not padded as required by the [PaddingOption]
     *   set for this [Base64] instance, or when there are extra symbols after the padding.
     *
     * @return a [ByteArray] with the resulting bytes.
     *
     * @sample samples.io.encoding.Base64Samples.decodeFromStringSample
     */
    public fun decode(source: CharSequence, startIndex: Int = 0, endIndex: Int = source.length): ByteArray {
        val byteSource = platformCharsToBytes(source, startIndex, endIndex)
        return decode(byteSource)
    }

    /**
     * Decodes symbols from the specified [source] char sequence or its substring and writes resulting bytes into the [destination] array.
     * Returns the number of bytes written.
     *
     * The requirement, prohibition, or optionality of padding in the input symbols
     * is determined by the [PaddingOption] set for this [Base64] instance.
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
     * @throws IllegalArgumentException when the symbols for decoding are not padded as required by the [PaddingOption]
     *   set for this [Base64] instance, or when there are extra symbols after the padding.
     *
     * @return the number of bytes written into [destination] array.
     *
     * @sample samples.io.encoding.Base64Samples.decodeIntoByteArraySample
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
        val _ = encodeIntoByteArrayImpl(source, destination, 0, startIndex, endIndex)
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
                if (shouldPadOnEncode()) {
                    destination[destinationIndex++] = padSymbol
                    destination[destinationIndex++] = padSymbol
                }
            }
            2 -> {
                val byte1 = source[sourceIndex++].toInt() and 0xFF
                val byte2 = source[sourceIndex++].toInt() and 0xFF
                val bits = (byte1 shl 10) or (byte2 shl 2)
                destination[destinationIndex++] = encodeMap[bits ushr 12]
                destination[destinationIndex++] = encodeMap[(bits ushr 6) and 0x3F]
                destination[destinationIndex++] = encodeMap[bits and 0x3F]
                if (shouldPadOnEncode()) {
                    destination[destinationIndex++] = padSymbol
                }
            }
        }

        check(sourceIndex == endIndex)

        return destinationIndex - destinationOffset
    }

    // internal for testing
    internal fun encodeSize(sourceSize: Int): Int {
        // includes padding chars if shouldPadOnEncode
        val groups = sourceSize / bytesPerGroup
        val trailingBytes = sourceSize % bytesPerGroup
        var size = groups * symbolsPerGroup
        if (trailingBytes != 0) { // trailing symbols
            size += if (shouldPadOnEncode()) symbolsPerGroup else trailingBytes + 1
        }
        if (size < 0) { // Int overflow
            throw IllegalArgumentException("Input is too big")
        }
        if (isMimeScheme) { // line separators
            size += ((size - 1) / mimeLineLength) * 2
        }
        if (size < 0) { // Int overflow
            throw IllegalArgumentException("Input is too big")
        }
        return size
    }

    private fun shouldPadOnEncode(): Boolean =
        paddingOption == PaddingOption.PRESENT || paddingOption == PaddingOption.PRESENT_OPTIONAL

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
        var hasPadding = false

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
                    hasPadding = true
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
        if (byteStart != -bitsPerByte && !hasPadding && paddingOption == PaddingOption.PRESENT) {
            throw IllegalArgumentException("The padding option is set to PRESENT, but the input is not properly padded")
        }
        if (payload != 0) { // the pad bits are non-zero
            throw IllegalArgumentException("The pad bits must be zeros")
        }

        sourceIndex = skipIllegalSymbolsIfMime(source, sourceIndex, endIndex)
        if (sourceIndex < endIndex) {
            val symbol = source[sourceIndex].toInt() and 0xFF
            throw IllegalArgumentException("Symbol '${symbol.toChar()}'(${symbol.toString(radix = 8)}) at index ${sourceIndex - 1} is prohibited after the pad character")
        }

        return destinationIndex - destinationOffset
    }

    // internal for testing
    internal fun decodeSize(source: ByteArray, startIndex: Int, endIndex: Int): Int {
        var symbols = endIndex - startIndex
        if (symbols == 0) {
            return 0
        }
        if (symbols == 1) {
            throw IllegalArgumentException("Input should have at least 2 symbols for Base64 decoding, startIndex: $startIndex, endIndex: $endIndex")
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
                checkPaddingIsAllowed(padIndex)
                val secondPadIndex = skipIllegalSymbolsIfMime(source, padIndex + 1, endIndex)
                if (secondPadIndex == endIndex || source[secondPadIndex] != padSymbol) {
                    throw IllegalArgumentException("Missing one pad character at index $secondPadIndex")
                }
                secondPadIndex + 1
            }
            -bitsPerByte + 3 * bitsPerSymbol - 2 * bitsPerByte -> { // xxx=
                checkPaddingIsAllowed(padIndex)
                padIndex + 1
            }
            else ->
                error("Unreachable")
        }
    }

    private fun checkPaddingIsAllowed(padIndex: Int) {
        if (paddingOption == PaddingOption.ABSENT) {
            throw IllegalArgumentException(
                "The padding option is set to ABSENT, but the input has a pad character at index $padIndex"
            )
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
     * Uses "The Base 64 Alphabet" as specified in Table 1 of RFC 4648 for encoding and decoding, consisting of
     * `'A'..'Z'`, `'a'..'z'`, `'+'` and `'/'` characters.
     *
     * This instance is configured with the padding option set to [PaddingOption.PRESENT].
     * Use the [withPadding] function to create a new instance with a different padding option if necessary.
     *
     * Encode operation does not add any line separator character.
     * Decode operation throws if it encounters a character outside the base64 alphabet.
     *
     * @sample samples.io.encoding.Base64Samples.defaultEncodingSample
     */
    public companion object Default : Base64(isUrlSafe = false, isMimeScheme = false, mimeLineLength = -1, paddingOption = PaddingOption.PRESENT) {

        private const val bitsPerByte: Int = 8
        private const val bitsPerSymbol: Int = 6

        internal const val bytesPerGroup: Int = 3
        internal const val symbolsPerGroup: Int = 4

        internal const val padSymbol: Byte = 61 // '='

        private const val lineLengthMime: Int = 76
        private const val lineLengthPem: Int = 64
        internal val mimeLineSeparatorSymbols: ByteArray = byteArrayOf('\r'.code.toByte(), '\n'.code.toByte())

        /**
         * The "base64url" encoding specified by [`RFC 4648 section 5`](https://www.rfc-editor.org/rfc/rfc4648#section-5),
         * Base 64 Encoding with URL and Filename Safe Alphabet.
         *
         * Uses "The URL and Filename safe Base 64 Alphabet" as specified in Table 2 of RFC 4648 for encoding and decoding,
         * consisting of `'A'..'Z'`, `'a'..'z'`, `'-'` and `'_'` characters.
         *
         * This instance is configured with the padding option set to [PaddingOption.PRESENT].
         * Use the [withPadding] function to create a new instance with a different padding option if necessary.
         *
         * Encode operation does not add any line separator character.
         * Decode operation throws if it encounters a character outside the base64url alphabet.
         *
         * @sample samples.io.encoding.Base64Samples.urlSafeEncodingSample
         */
        public val UrlSafe: Base64 = Base64(isUrlSafe = true, isMimeScheme = false, mimeLineLength = -1, paddingOption = PaddingOption.PRESENT)

        /**
         * The encoding specified by [`RFC 2045 section 6.8`](https://www.rfc-editor.org/rfc/rfc2045#section-6.8),
         * Base64 Content-Transfer-Encoding.
         *
         * Uses "The Base64 Alphabet" as specified in Table 1 of RFC 2045 for encoding and decoding,
         * consisting of `'A'..'Z'`, `'a'..'z'`, `'+'` and `'/'` characters.
         *
         * This instance is configured with the padding option set to [PaddingOption.PRESENT].
         * Use the [withPadding] function to create a new instance with a different padding option if necessary.
         *
         * Encode operation adds CRLF every 76 symbols. No line separator is added to the end of the encoded output.
         * Decode operation ignores all line separators and other characters outside the base64 alphabet.
         *
         * @sample samples.io.encoding.Base64Samples.mimeEncodingSample
         */
        public val Mime: Base64 = Base64(isUrlSafe = false, isMimeScheme = true, mimeLineLength = lineLengthMime, paddingOption = PaddingOption.PRESENT)

        /**
         * The encoding specified by [`RFC 1421 section 4.3.2.4`](https://www.rfc-editor.org/rfc/rfc1421#section-4.3.2.4),
         * Base64 Content-Transfer-Encoding.
         *
         * Uses the encoding alphabet as specified in Table 1 of RFC 1421 for encoding and decoding,
         * consisting of `'A'..'Z'`, `'a'..'z'`, `'+'` and `'/'` characters.
         *
         * This instance is configured with the padding option set to [PaddingOption.PRESENT].
         * Use the [withPadding] function to create a new instance with a different padding option if necessary.
         *
         * Encode operation adds CRLF every 64 symbols. No line separator is added to the end of the encoded output.
         * Decode operation ignores all line separators and other characters outside the base64 alphabet.
         *
         * @sample samples.io.encoding.Base64Samples.pemEncodingSample
         */
        public val Pem: Base64 = Base64(isUrlSafe = false, isMimeScheme = true, mimeLineLength = lineLengthPem, paddingOption = PaddingOption.PRESENT)
    }
}


// "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/"
private val base64EncodeMap = byteArrayOf(
    65,  66,  67,  68,  69,  70,  71,  72,  73,  74,  75,  76,  77,  78,  79,  80,  /* 0 - 15 */
    81,  82,  83,  84,  85,  86,  87,  88,  89,  90,  97,  98,  99,  100, 101, 102, /* 16 - 31 */
    103, 104, 105, 106, 107, 108, 109, 110, 111, 112, 113, 114, 115, 116, 117, 118, /* 32 - 47 */
    119, 120, 121, 122, 48,  49,  50,  51,  52,  53,  54,  55,  56,  57,  43,  47,  /* 48 - 63 */
)

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

private val base64UrlDecodeMap = IntArray(256).apply {
    this.fill(-1)
    this[Base64.padSymbol.toInt()] = -2
    base64UrlEncodeMap.forEachIndexed { index, symbol ->
        this[symbol.toInt()] = index
    }
}


@SinceKotlin("1.8")
internal fun isInMimeAlphabet(symbol: Int): Boolean {
    return symbol in base64DecodeMap.indices && base64DecodeMap[symbol] != -1
}


@SinceKotlin("1.8")
internal expect fun Base64.platformCharsToBytes(
    source: CharSequence,
    startIndex: Int,
    endIndex: Int
): ByteArray


@SinceKotlin("1.8")
internal expect fun Base64.platformEncodeToString(
    source: ByteArray,
    startIndex: Int,
    endIndex: Int
): String

@SinceKotlin("1.8")
internal expect fun Base64.platformEncodeIntoByteArray(
    source: ByteArray,
    destination: ByteArray,
    destinationOffset: Int,
    startIndex: Int,
    endIndex: Int
): Int

@SinceKotlin("1.8")
internal expect fun Base64.platformEncodeToByteArray(
    source: ByteArray,
    startIndex: Int,
    endIndex: Int
): ByteArray
