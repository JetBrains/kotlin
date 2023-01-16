/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:JvmMultifileClass
@file:JvmName("StreamEncodingKt")

package kotlin.io.encoding

import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import kotlin.io.encoding.Base64.Default.bytesPerGroup
import kotlin.io.encoding.Base64.Default.mimeLineLength
import kotlin.io.encoding.Base64.Default.mimeLineSeparatorSymbols
import kotlin.io.encoding.Base64.Default.padSymbol
import kotlin.io.encoding.Base64.Default.symbolsPerGroup

/**
 * Returns an input stream that decodes symbols from this input stream using the specified [base64] encoding.
 *
 * Reading from the returned input stream leads to reading some symbols from the underlying input stream.
 * The symbols are decoded using the specified [base64] encoding and the resulting bytes are returned.
 * Symbols are decoded in 4-symbol blocks.
 *
 * The symbols for decoding are not required to be padded.
 * However, if there is a padding character present, the correct amount of padding character(s) must be present.
 * The padding character `'='` is interpreted as the end of the symbol stream. Subsequent symbols are not read even if
 * the end of the underlying input stream is not reached.
 *
 * The returned input stream should be closed in a timely manner. We suggest you try the [use] function,
 * which closes the resource after a given block of code is executed.
 * The close operation discards leftover bytes.
 * Closing the returned input stream will close the underlying input stream.
 */
@SinceKotlin("1.8")
@ExperimentalEncodingApi
public fun InputStream.decodingWith(base64: Base64): InputStream {
    return DecodeInputStream(this, base64)
}

/**
 * Returns an output stream that encodes bytes using the specified [base64] encoding
 * and writes the result to this output stream.
 *
 * The byte data written to the returned output stream is encoded using the specified [base64] encoding
 * and the resulting symbols are written to the underlying output stream.
 * Bytes are encoded in 3-byte blocks.
 *
 * The returned output stream should be closed in a timely manner. We suggest you try the [use] function,
 * which closes the resource after a given block of code is executed.
 * The close operation writes properly padded leftover symbols to the underlying output stream.
 * Closing the returned output stream will close the underlying output stream.
 */
@SinceKotlin("1.8")
@ExperimentalEncodingApi
public fun OutputStream.encodingWith(base64: Base64): OutputStream {
    return EncodeOutputStream(this, base64)
}


@ExperimentalEncodingApi
private class DecodeInputStream(
    private val input: InputStream,
    private val base64: Base64
) : InputStream() {
    private var isClosed = false
    private var isEOF = false
    private val singleByteBuffer = ByteArray(1)

    private val symbolBuffer = ByteArray(1024)  // a multiple of symbolsPerGroup

    private val byteBuffer = ByteArray(1024)
    private var byteBufferStartIndex = 0
    private var byteBufferEndIndex = 0
    private val byteBufferLength: Int
        get() = byteBufferEndIndex - byteBufferStartIndex

    override fun read(): Int {
        if (byteBufferStartIndex < byteBufferEndIndex) {
            val byte = byteBuffer[byteBufferStartIndex].toInt() and 0xFF
            byteBufferStartIndex += 1
            resetByteBufferIfEmpty()
            return byte
        }
        return when (read(singleByteBuffer, 0, 1)) {
            -1 -> -1
            1 -> singleByteBuffer[0].toInt() and 0xFF
            else -> error("Unreachable")
        }
    }

    override fun read(destination: ByteArray, offset: Int, length: Int): Int {
        if (offset < 0 || length < 0 || offset + length > destination.size) {
            throw IndexOutOfBoundsException("offset: $offset, length: $length, buffer size: ${destination.size}")
        }
        if (isClosed) {
            throw IOException("The input stream is closed.")
        }
        if (isEOF) {
            return -1
        }
        if (length == 0) {
            return 0
        }

        if (byteBufferLength >= length) {
            copyByteBufferInto(destination, offset, length)
            return length
        }

        val bytesNeeded = length - byteBufferLength
        val groupsNeeded = (bytesNeeded + bytesPerGroup - 1) / bytesPerGroup
        var symbolsNeeded = groupsNeeded * symbolsPerGroup

        var dstOffset = offset

        while (!isEOF && symbolsNeeded > 0) {
            var symbolBufferLength = 0
            val symbolsToRead = minOf(symbolBuffer.size, symbolsNeeded)

            while (!isEOF && symbolBufferLength < symbolsToRead) {
                when (val symbol = readNextSymbol()) {
                    -1 ->
                        isEOF = true
                    padSymbol.toInt() -> {
                        symbolBufferLength = handlePaddingSymbol(symbolBufferLength)
                        isEOF = true
                    }
                    else -> {
                        symbolBuffer[symbolBufferLength] = symbol.toByte()
                        symbolBufferLength += 1
                    }
                }
            }

            check(isEOF || symbolBufferLength == symbolsToRead)

            symbolsNeeded -= symbolBufferLength

            dstOffset += decodeSymbolBufferInto(destination, dstOffset, length + offset, symbolBufferLength)
        }

        return if (dstOffset == offset && isEOF) -1 else dstOffset - offset
    }

    override fun close() {
        if (!isClosed) {
            isClosed = true
            input.close()
        }
    }

    // private functions

    private fun decodeSymbolBufferInto(dst: ByteArray, dstOffset: Int, dstEndIndex: Int, symbolBufferLength: Int): Int {
        byteBufferEndIndex += base64.decodeIntoByteArray(
            symbolBuffer,
            byteBuffer,
            destinationOffset = byteBufferEndIndex,
            startIndex = 0,
            endIndex = symbolBufferLength
        )

        val bytesToCopy = minOf(byteBufferLength, dstEndIndex - dstOffset)
        copyByteBufferInto(dst, dstOffset, bytesToCopy)
        shiftByteBufferToStartIfNeeded()
        return bytesToCopy
    }

    private fun copyByteBufferInto(dst: ByteArray, dstOffset: Int, length: Int) {
        byteBuffer.copyInto(
            dst,
            dstOffset,
            startIndex = byteBufferStartIndex,
            endIndex = byteBufferStartIndex + length
        )
        byteBufferStartIndex += length
        resetByteBufferIfEmpty()
    }

    private fun resetByteBufferIfEmpty() {
        if (byteBufferStartIndex == byteBufferEndIndex) {
            byteBufferStartIndex = 0
            byteBufferEndIndex = 0
        }
    }

    private fun shiftByteBufferToStartIfNeeded() {
        // byte buffer should always have enough capacity to accommodate all symbols from symbol buffer
        val byteBufferCapacity = byteBuffer.size - byteBufferEndIndex
        val symbolBufferCapacity = symbolBuffer.size / symbolsPerGroup * bytesPerGroup
        if (symbolBufferCapacity > byteBufferCapacity) {
            byteBuffer.copyInto(byteBuffer, 0, byteBufferStartIndex, byteBufferEndIndex)
            byteBufferEndIndex -= byteBufferStartIndex
            byteBufferStartIndex = 0
        }
    }

    private fun handlePaddingSymbol(symbolBufferLength: Int): Int {
        symbolBuffer[symbolBufferLength] = padSymbol

        return when (symbolBufferLength and 3) { // pads expected
            2 -> { // xx=
                val secondPad = readNextSymbol()
                if (secondPad >= 0) {
                    symbolBuffer[symbolBufferLength + 1] = secondPad.toByte()
                }
                symbolBufferLength + 2
            }
            else ->
                symbolBufferLength + 1
        }
    }

    private fun readNextSymbol(): Int {
        if (!base64.isMimeScheme) {
            return input.read()
        }

        var read: Int
        do {
            read = input.read()
        } while (read != -1 && !isInMimeAlphabet(read))

        return read
    }
}

@ExperimentalEncodingApi
private class EncodeOutputStream(
    private val output: OutputStream,
    private val base64: Base64
) : OutputStream() {
    private var isClosed = false

    private var lineLength = if (base64.isMimeScheme) mimeLineLength else -1

    private val symbolBuffer = ByteArray(1024)

    private val byteBuffer = ByteArray(bytesPerGroup)
    private var byteBufferLength = 0

    override fun write(b: Int) {
        checkOpen()
        byteBuffer[byteBufferLength++] = b.toByte()
        if (byteBufferLength == bytesPerGroup) {
            encodeByteBufferIntoOutput()
        }
    }

    override fun write(source: ByteArray, offset: Int, length: Int) {
        checkOpen()
        if (offset < 0 || length < 0 || offset + length > source.size) {
            throw IndexOutOfBoundsException("offset: $offset, length: $length, source size: ${source.size}")
        }
        if (length == 0) {
            return
        }

        check(byteBufferLength < bytesPerGroup)

        var startIndex = offset
        val endIndex = startIndex + length

        if (byteBufferLength != 0) {
            startIndex += copyIntoByteBuffer(source, startIndex, endIndex)
            if (byteBufferLength != 0) {
                return
            }
        }

        while (startIndex + bytesPerGroup <= endIndex) {
            val groupCapacity = (if (base64.isMimeScheme) lineLength else symbolBuffer.size) / symbolsPerGroup
            val groupsToEncode = minOf(groupCapacity, (endIndex - startIndex) / bytesPerGroup)
            val bytesToEncode = groupsToEncode * bytesPerGroup

            val symbolsEncoded = encodeIntoOutput(source, startIndex, startIndex + bytesToEncode)
            check(symbolsEncoded == groupsToEncode * symbolsPerGroup)

            startIndex += bytesToEncode
        }

        source.copyInto(byteBuffer, destinationOffset = 0, startIndex, endIndex)
        byteBufferLength = endIndex - startIndex
    }

    override fun flush() {
        checkOpen()
        output.flush()
    }

    override fun close() {
        if (!isClosed) {
            isClosed = true
            if (byteBufferLength != 0) {
                encodeByteBufferIntoOutput()
            }
            output.close()
        }
    }

    // private functions

    private fun copyIntoByteBuffer(source: ByteArray, startIndex: Int, endIndex: Int): Int {
        val bytesToCopy = minOf(bytesPerGroup - byteBufferLength, endIndex - startIndex)
        source.copyInto(byteBuffer, destinationOffset = byteBufferLength, startIndex, startIndex + bytesToCopy)
        byteBufferLength += bytesToCopy
        if (byteBufferLength == bytesPerGroup) {
            encodeByteBufferIntoOutput()
        }
        return bytesToCopy
    }

    private fun encodeByteBufferIntoOutput() {
        val symbolsEncoded = encodeIntoOutput(byteBuffer, 0, byteBufferLength)
        check(symbolsEncoded == symbolsPerGroup)
        byteBufferLength = 0
    }

    private fun encodeIntoOutput(source: ByteArray, startIndex: Int, endIndex: Int): Int {
        val symbolsEncoded = base64.encodeIntoByteArray(
            source,
            symbolBuffer,
            destinationOffset = 0,
            startIndex,
            endIndex
        )
        if (lineLength == 0) {
            output.write(mimeLineSeparatorSymbols)
            lineLength = mimeLineLength
            check(symbolsEncoded <= mimeLineLength)
        }
        output.write(symbolBuffer, 0, symbolsEncoded)
        lineLength -= symbolsEncoded
        return symbolsEncoded
    }

    private fun checkOpen() {
        if (isClosed) throw IOException("The output stream is closed.")
    }
}