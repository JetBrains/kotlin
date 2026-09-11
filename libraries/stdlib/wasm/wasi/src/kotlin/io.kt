/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:OptIn(ExperimentalUnsignedTypes::class)

package kotlin.io

import stdlib.wit.bindings.Streams
import stdlib.wit.bindings.runtime.ComponentException
import kotlin.wasm.unsafe.MemoryAllocator
import kotlin.wasm.unsafe.withScopedMemoryAllocator

private const val BUFFER_SIZE: Int = 32

private const val CR: Byte = 0x0D.toByte()
private const val LF: Byte = 0x0A.toByte()

@OptIn(ExperimentalWasmInterop::class)
private fun wasiPrintImpl(
    allocator: MemoryAllocator,
    data: ByteArray?,
    newLine: Boolean,
    useErrorStream: Boolean,
) {
    val dataSize: ULong = data?.size?.toULong() ?: 0u
    val bytesToWrite: ULong = dataSize + (if (newLine) 1u else 0u)
    if (bytesToWrite == 0uL)
        return

    val ostream = if (useErrorStream)
        stdlib.wit.bindings.Stderr.getStderr()
    else
        stdlib.wit.bindings.Stdout.getStdout()

    var written = 0u.toULong()
    while (written < bytesToWrite) {
        val allowedToWrite = ostream.checkWrite();
        if (allowedToWrite.isFailure)
            throw WasiError("Cannot write to this stream", allowedToWrite.exceptionOrNull()!!)

        val allowedBytesToWrite = allowedToWrite.getOrThrow()

        // NOTE: polling in this case is the correct behavior, also see wasi-libc: https://github.com/WebAssembly/wasi-libc/blob/79c1a738e1b3df432545666a1a579c49a82b4514/libc-bottom-half/sources/file_utils.c#L487
        if (allowedBytesToWrite == 0uL)
            continue

        val remainingBytesToWrite = bytesToWrite - written
        val actualBytesToWriteRightNow = minOf(remainingBytesToWrite, allowedBytesToWrite)

        val listToWrite = ArrayList<UByte>(actualBytesToWriteRightNow.toInt())
        for (i in written until written + actualBytesToWriteRightNow) {
            if (data != null && i < data.size.toULong()) {
                // TODO(REVIEW) could optimize this to not use the high-level wit-bindgen generated write, but instead directly use the __wasm_import_write function and do canonical ABI related stuff by hand. Would save one set of list copies.
                listToWrite.add(data[i.toInt()].toUByte())
            } else {
                // TODO(REVIEW) probably delete the assert?
                assert(newLine)
                // NOTE: this also takes care of the case in which data was null to begin with
                listToWrite.add('\n'.code.toUByte())
            }

        }

        // NOTE: does not flush, this is done manually after the loop
        // TODO(REVIEW): to optimize this, could perform an `ostream.blockingWriteAndFlush()` if this write is the last one, and less than 4096 bytes, then we wouldn't need the separate blockingFlush() call afterwards
        val res = ostream.write(listToWrite);

        if (res.isFailure) // NOTE: this can most likely only occur in a TOCTOU case, where, e.g., the stream has closed since we called `checkWrite()`
            throw WasiError("WASI stream did not accept write, even though `checkWrite()` permitted it", res.exceptionOrNull()!!)

        written += actualBytesToWriteRightNow
    }

    // manually flush, as we can't rely on having written a newline at the end
    // NOTE: this result has a unit type on success, i.e. doesn't have a success value
    val ret = ostream.blockingFlush()
    if (ret.isFailure)
        throw WasiError(null, ret.exceptionOrNull()!!)
}

private fun printImpl(message: String?, useErrorStream: Boolean, newLine: Boolean) {
    withScopedMemoryAllocator { allocator ->
        wasiPrintImpl(
            allocator = allocator,
            data = message?.encodeToByteArray(),
            newLine = newLine,
            useErrorStream = useErrorStream,
        )
    }
}

internal actual fun printError(error: String?) {
    printImpl(error, useErrorStream = true, newLine = false)
}

/** Prints the line separator to the standard output stream. */
public actual fun println() {
    printImpl(null, useErrorStream = false, newLine = true)
}

/** Prints the given [message] and the line separator to the standard output stream. */
public actual fun println(message: Any?) {
    printImpl(message?.toString(), useErrorStream = false, newLine = true)
}

/** Prints the given [message] to the standard output stream. */
public actual fun print(message: Any?) {
    printImpl(message?.toString(), useErrorStream = false, newLine = false)
}

@OptIn(ExperimentalWasmInterop::class)
private fun wasiReadLineImpl(): ByteArray? {
    // use a linked list of fixed-size buffers to avoid too many copies
    val arrayBuffers = mutableListOf<ByteArray>()
    var currentBuffer = ByteArray(BUFFER_SIZE)
    var currentBufferIndex = 0

    val stdinStr = stdlib.wit.bindings.Stdin.getStdin()

    while (true) {
        // NOTE: we can only read one byte at a time, as we need to be able to detect a newline. We cannot "put back" any bytes into the stream, so reading anything more than the next \n would simply be incorrect
        //       (Also note that internally buffering the stream wouldn't help: a) we'd still consume it from the outside, and b) if the user accesses the stream through raw wasi calls, they won't see our Kotlin-specfic buffer)
        val ret = stdinStr.blockingRead(1u)
        if (ret.isFailure) {
            val componentExcn = ret.exceptionOrNull()!! as ComponentException
            val streamError = componentExcn.value as Streams.StreamError

            // end of file / stream otherwise closed
            // this is the exact case that the null return value represents here
            if (streamError == Streams.StreamError.Closed)
                return null

            throw WasiError("WASI stream read failed", ret.exceptionOrNull()!!)
        }

        // TODO(REVIEW): Technically this is one check too much, as we already know ret.value is valid, so we could just do `ret as List<UByte>`. Just a big ugly
        val returnedListOfBytes = ret.getOrThrow()

        val readSize = returnedListOfBytes.size
        check(readSize == 0 || readSize == 1) { "Unexpected WASI result" }
        if (readSize == 0 && currentBufferIndex == 0 && arrayBuffers.isEmpty()) return null

        fun finish(): ByteArray {
            // don't put an ending CR (\r) of a potential \n\r into the buffer
            if (currentBufferIndex > 0 && currentBuffer[currentBufferIndex - 1] == CR) {
                currentBufferIndex--
            }

            val resultSize = arrayBuffers.size * BUFFER_SIZE + currentBufferIndex
            val result = ByteArray(resultSize)
            // concatenate all the buffers together
            arrayBuffers.forEachIndexed { index, array ->
                array.copyInto(destination = result, destinationOffset = index * BUFFER_SIZE)
            }
            currentBuffer.copyInto(
                destination = result,
                destinationOffset = arrayBuffers.size * BUFFER_SIZE,
                endIndex = currentBufferIndex
            )
            return result
        }

        if (readSize == 0)
            return finish()

        // convert to Byte, which doesn't change the binary representation. This allows us to use ByteArray.decodeToString()
        val nextByte: Byte = returnedListOfBytes[0].toByte()
        if (nextByte == LF)
            return finish()

        if (currentBufferIndex >= BUFFER_SIZE) {
            // save the buffer as "done", and start reading into a new one
            arrayBuffers.add(currentBuffer)
            currentBuffer = ByteArray(BUFFER_SIZE)
            currentBufferIndex = 0
        }

        currentBuffer[currentBufferIndex] = nextByte
        currentBufferIndex++
    }
}

/**
 * Reads a line of input from the standard input stream and returns it,
 * or throws a [RuntimeException] if EOF has already been reached when [readln] is called.
 *
 * LF or CRLF is treated as the line terminator. Line terminator is not included in the returned string.
 *
 * The input is decoded using the system default Charset. A [CharacterCodingException] is thrown if input is malformed.
 */
@SinceKotlin("1.6")
public actual fun readln(): String = readlnOrNull() ?: throw ReadAfterEOFException("EOF has already been reached")

/**
 * Reads a line of input from the standard input stream and returns it,
 * or return `null` if EOF has already been reached when [readlnOrNull] is called.
 *
 * LF or CRLF is treated as the line terminator. Line terminator is not included in the returned string.
 *
 * The input is decoded using the system default Charset. A [CharacterCodingException] is thrown if input is malformed.
 */
@SinceKotlin("1.6")
public actual fun readlnOrNull(): String? {
    return wasiReadLineImpl()?.decodeToString()
}
