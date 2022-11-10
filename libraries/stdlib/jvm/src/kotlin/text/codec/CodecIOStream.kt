/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("UnusedReceiverParameter")

package kotlin.text.codec

import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
//import java.io.Reader
//import java.io.Writer


//public fun Reader.wrapForDecoding(codec: BaseNCodec): InputStream { TODO() }
//public fun Writer.wrapForEncoding(codec: BaseNCodec): OutputStream { TODO() }

/**
 * Returns an input stream that wraps this input stream for decoding symbol stream using the specified [codec].
 *
 * Reading from the returned input stream leads to reading some symbols from the underlying input stream.
 * The symbols are decoded using the specified [codec] and the resulting bytes are returned.
 *
 * Closing the returned input stream will close the underlying input stream.
 */
public fun InputStream.wrapForDecoding(codec: BaseNCodec): InputStream {
    return CodecInputStream(this, codec)
}

/**
 * Returns an output stream that wraps this output stream for encoding byte data using the specified [codec].
 *
 * The byte data written to the returned output stream is encoded using the specified [codec]
 * and the resulting symbols are written to the underlying output stream.
 *
 * The returned output stream should be promptly closed after use,
 * during which it will flush all possible leftover symbols to the underlying
 * output stream. Closing the returned output stream will close the underlying
 * output stream.
 */
public fun OutputStream.wrapForEncoding(codec: BaseNCodec): OutputStream {
    return CodecOutputStream(this, codec)
}


private class CodecInputStream(
    private val input: InputStream,
    codec: BaseNCodec
) : InputStream() {

    private val decodeMap: ByteArray = codec.decodeMap

    private val bitsPerByte = Byte.SIZE_BITS
    private val bitsPerSymbol = codec.bitsPerSymbol

    private val paddingSymbol: Int = codec.paddingSymbol.toInt()

    private var isClosed = false
    private var isEOF = false
    private val singleByteBuffer = ByteArray(1)

    private var payload = 0
    private var byteStart = -bitsPerByte

    private fun throwIllegalSymbol(symbol: Int): Nothing {
        throw throw IllegalArgumentException("Invalid symbol '${symbol.toChar()}'(${symbol.toString(radix = 8)}).")
    }

    private fun decodeSymbol(symbol: Int): Int {
        val bits = decodeMap[symbol]
        if (bits < 0) {
            throwIllegalSymbol(symbol)
        }
        return bits.toInt()
    }

    override fun read(): Int {
        return if (read(singleByteBuffer, 0, 1) == -1) -1 else singleByteBuffer[0].toInt() and 0xFF
    }

    override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
        if (isClosed) {
            throw IOException("The input stream is closed.")
        }
        if (isEOF) {
            return -1
        }
        if (length == 0) {
            return 0
        }

        var bufferIndex = 0

        while (bufferIndex < length) {
            var symbol = input.read()

            if (symbol == -1) {
                isEOF = true
//                if (byteStart != -codec.bitsPerByte) {
//                    val remainingSymbols = (byteStart + codec.bitsPerByte) / codec.bitsPerSymbol
//                    throw IOException("Reached end of stream, but $remainingSymbols un-decoded symbols are left.")
//                }

                check(payload == 0) { "Reached end of stream, but payload is not empty." }
                break
            }
            if (symbol == paddingSymbol) {
                while (symbol == paddingSymbol) {
                    symbol = input.read()
                }
                if (symbol != -1) {
                    throw IOException("End of stream expected, but encountered symbol '${symbol.toChar()}'(${symbol.toString(radix = 8)})")
                }
                isEOF = true

                check(payload == 0) { "Reached end of stream, but payload is not empty." }
                break
            }

            payload = (payload shl bitsPerSymbol) or decodeSymbol(symbol)
            byteStart += bitsPerSymbol

            if (byteStart >= 0) {
                buffer[offset + bufferIndex] = (payload ushr byteStart).toByte()
                bufferIndex += 1

                payload = payload and ((1 shl byteStart) - 1)
                byteStart -= bitsPerByte
            }
        }

        return if (bufferIndex == 0) {
            check(isEOF)
            -1
        } else {
            bufferIndex
        }
    }

    override fun available(): Int {
        if (isClosed) {
            return 0
        }
        return (input.available() * bitsPerSymbol) / bitsPerByte
    }

    override fun close() {
        if (!isClosed) {
            isClosed = true
            input.close()
        }
    }
}

private class CodecOutputStream(
    private val output: OutputStream,
    codec: BaseNCodec
) : OutputStream() {

    private val encodeMap: ByteArray = codec.encodeMap

    private val bitsPerByte = Byte.SIZE_BITS
    private val bitsPerSymbol = codec.bitsPerSymbol

    private val paddingSymbol: Int = codec.paddingSymbol.toInt()

    private var isClosed = false
    private val singleByteBuffer = ByteArray(1)
    private val buffer = ByteArray(4096)

    private var payload = 0
    private var symbolStart = -bitsPerSymbol

    override fun write(b: Int) {
        singleByteBuffer[0] = b.toByte()
        write(singleByteBuffer, 0, 1)
    }

    private fun symbolAt(index: Int): Byte = encodeMap[index]

    override fun write(source: ByteArray, offset: Int, length: Int) {
        if (isClosed) {
            throw IOException("The output stream is closed.")
        }

        var bufferIndex = 0
        for (sourceIndex in 0 until length) {
            val byte = source[sourceIndex + offset]

            payload = (payload shl bitsPerByte) or byte.toInt()
            symbolStart += bitsPerByte

            while (symbolStart >= 0) {
                buffer[bufferIndex++] = symbolAt(payload ushr symbolStart)

                if (bufferIndex == buffer.size) {
                    output.write(buffer, 0, bufferIndex)
                    bufferIndex = 0
                }

                payload = payload and ((1 shl symbolStart) - 1)
                symbolStart -= bitsPerSymbol
            }
        }

        check(symbolStart < 0)
        if (symbolStart == -bitsPerSymbol) {
            check(payload == 0)
        }

        output.write(buffer, 0, bufferIndex)
    }

    override fun flush() {
        output.flush()
    }

    override fun close() {
        if (!isClosed) {
            isClosed = true
            if (symbolStart == -bitsPerSymbol) {
                return
            }
            output.write(symbolAt(payload shl -symbolStart).toInt()) // and 0xFF

            while (symbolStart % bitsPerSymbol != 0) {
                symbolStart += bitsPerByte
            }
            repeat(symbolStart / bitsPerSymbol) {
                output.write(paddingSymbol)
            }

            payload = 0
            symbolStart = -bitsPerSymbol
            output.close()
        }
    }
}