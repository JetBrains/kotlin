/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:OptIn(ExperimentalUnsignedTypes::class)

package kotlin.io

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
    // TODO probably un-ulong all of this, and just convert only when necessary
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
        if (allowedToWrite.isFailure) {
            // TODO proper error handling
            // TODO this is the case where we're not allowed to write anything, but not sure that's actually always an error
            throw WasiP2Error(allowedToWrite.exceptionOrNull()!!)
        }

        val allowedBytesToWrite = allowedToWrite.getOrThrow()

        // TODO should we really poll in this case?
        if (allowedBytesToWrite == 0uL)
            continue

        val actualBytesToWriteRightNow = minOf(bytesToWrite, allowedBytesToWrite)

        val listToWrite = ArrayList<UByte>(actualBytesToWriteRightNow.toInt())
        for (i in written until written + actualBytesToWriteRightNow) {
            if (data != null && i < data.size.toULong())
            // TODO maybe optimize if possible so that hopefully some part of the compiler can vectorize this
                listToWrite.add(data[i.toInt()].toUByte())
            else {
                // TODO probably delete the assert?
                assert(newLine)
                // NOTE: this also takes care of the case in which data was null to begin with
                listToWrite.add('\n'.code.toUByte())
            }

        }

        // TODO instead of flushing manually later, could perform an `ostream.blockingWriteAndFlush()` if this is the last one, and less than 4096 bytes
        val res = ostream.write(listToWrite);

        if (res.isFailure)
        // TODO proper errors
        // TODO stream has closed since we've been given permission to write to it (TOCTOU failure)
            throw WasiP2Error(res.exceptionOrNull()!!)

        written += actualBytesToWriteRightNow
    }

    // manually flush, as we can't rely on having written a newline at the end
    // TODO what to do with the result
    val ret = ostream.blockingFlush()
    if (ret.isFailure)
        throw WasiP2Error(ret.exceptionOrNull()!!)
    /*
    val _ = ostream.flush()
    // this doesn't block, so need to poll checkWrite, but TODO test this more thoroughly
    // NOTE that because of the semantics of flush, this also stops polling when checkWrite returns failure (converted to null by getOrNull)
    @Suppress("ControlFlowWithEmptyBody")
    while (ostream.checkWrite().getOrNull() == 0uL);
     */
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

    // TODO test for EOF

    while (true) {
        // TODO read more than one at a time? But can't really "put them back", so then would need to internally buffer them, which might not be desirable, could lead to strange semantics. And if the user accesses the stream through raw wasi calls, it will also be super strange. Blocking would also have to be reconsidered, couldn't blocking read more than a line.
        val ret = stdinStr.blockingRead(1u)
        if (ret.isFailure)
            throw WasiP2Error(ret.exceptionOrNull()!!) // TODO proper errors

        // TODO maybe optimize? use value directly?
        val returnedListOfBytes = ret.getOrThrow()

        val readSize = returnedListOfBytes.size
        check(readSize == 0 || readSize == 1) { "Unexpected WASI result" }
        if (readSize == 0 && currentBufferIndex == 0 && arrayBuffers.isEmpty()) return null

        val finish = {
            if (currentBufferIndex > 0 && currentBuffer[currentBufferIndex - 1] == CR) {
                currentBufferIndex--
            }

            val resultSize = arrayBuffers.size * BUFFER_SIZE + currentBufferIndex
            val result = ByteArray(resultSize)
            arrayBuffers.forEachIndexed { index, array ->
                array.copyInto(destination = result, destinationOffset = index * BUFFER_SIZE)
            }
            currentBuffer.copyInto(
                destination = result,
                destinationOffset = arrayBuffers.size * BUFFER_SIZE,
                endIndex = currentBufferIndex
            )
            result
        }

        if (readSize == 0)
            return finish()

        // convert to Byte, which doesn't change the binary representation. This allows us to use ByteArray.decodeToString()
        val nextByte: Byte = returnedListOfBytes[0].toByte()
        if (nextByte == LF)
            return finish()

        if (currentBufferIndex >= BUFFER_SIZE) {
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
