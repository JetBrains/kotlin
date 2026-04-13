/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.io

import kotlin.wasm.WasiError
import kotlin.wasm.WasiErrorCode
import kotlin.wasm.WasmImport
import kotlin.wasm.ExperimentalWasmInterop
import kotlin.wasm.unsafe.MemoryAllocator
import kotlin.wasm.unsafe.withScopedMemoryAllocator

private const val STDIN = 0
private const val STDOUT = 1
private const val STDERR = 2

private const val USER_DATA = 0L
private const val EVENT_FD_READ = 1

private const val BUFFER_SIZE: Int = 32

private const val CR: Byte = 0x0D.toByte()
private const val LF: Byte = 0x0A.toByte()

/**
 * Write to a file descriptor. Note: This is similar to `writev` in POSIX.
 */
@ExperimentalWasmInterop
@WasmImport("wasi_snapshot_preview1", "fd_write")
private external fun wasiRawFdWrite(descriptor: Int, scatterPtr: Int, scatterSize: Int, errorPtr: Int): Int

/** Read from a file descriptor. Note: This is similar to `readv` in POSIX. */
@ExperimentalWasmInterop
@WasmImport("wasi_snapshot_preview1", "fd_read")
private external fun wasiRawFdRead(descriptor: Int, gatherPtr: Int, gatherSize: Int, errorPtr: Int): Int

/** Concurrently poll for the occurrence of a set of events. */
@ExperimentalWasmInterop
@WasmImport("wasi_snapshot_preview1", "poll_oneoff")
private external fun wasiPollOneOff(subscriptionPtr: Int, eventPtr: Int, nSubscriptions: Int, errorPtr: Int): Int

@OptIn(ExperimentalWasmInterop::class)
private fun wasiPrintImpl(
    allocator: MemoryAllocator,
    data: ByteArray?,
    newLine: Boolean,
    useErrorStream: Boolean
) {
    val dataSize = data?.size ?: 0
    val memorySize = dataSize + (if (newLine) 1 else 0)
    if (memorySize == 0) return

    val ptr = allocator.allocate(memorySize)
    if (data != null) {
        var currentPtr = ptr
        for (el in data) {
            currentPtr.storeByte(el)
            currentPtr += 1
        }
    }
    if (newLine) {
        (ptr + dataSize).storeByte(0x0A)
    }

    val scatterPtr = allocator.allocate(8)
    (scatterPtr + 0).storeInt(ptr.address.toInt())
    (scatterPtr + 4).storeInt(memorySize)

    val rp0 = allocator.allocate(4)

    val ret =
        wasiRawFdWrite(
            descriptor = if (useErrorStream) STDERR else STDOUT,
            scatterPtr = scatterPtr.address.toInt(),
            scatterSize = 1,
            errorPtr = rp0.address.toInt()
        )

    if (ret != 0) {
        throw WasiError(WasiErrorCode.entries[ret])
    }
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
private fun wasiWaitUntilUserInputImpl(allocator: MemoryAllocator) {
    val subscriptionPtr = allocator.allocate(20)
    (subscriptionPtr + 0).storeLong(USER_DATA)
    (subscriptionPtr + 8).storeByte(1)
    (subscriptionPtr + 16).storeInt(STDIN)

    val eventSize = 26
    val eventPtr = allocator.allocate(eventSize)

    val rp0 = allocator.allocate(4)

    val ret = wasiPollOneOff(
        subscriptionPtr = subscriptionPtr.address.toInt(),
        eventPtr = eventPtr.address.toInt(),
        nSubscriptions = 1,
        errorPtr = rp0.address.toInt()
    )
    if (ret != 0) {
        throw WasiError(WasiErrorCode.entries[ret])
    }

    val eventsCount = rp0.loadInt()
    check(eventsCount == 1) { "Unexpected WASI result" }
    val eventUserdata = (eventPtr + 0).loadLong()
    check(eventUserdata == USER_DATA) { "Unexpected WASI result" }
    val eventRet = (eventPtr + 8).loadShort().toInt()
    if (eventRet != 0) {
        throw WasiError(WasiErrorCode.entries[eventRet])
    }
    val eventType = (eventPtr + 10).loadByte().toInt()
    check(eventType == EVENT_FD_READ) { "Unexpected WASI result" }
}

@OptIn(ExperimentalWasmInterop::class)
private fun wasiReadLineImpl(allocator: MemoryAllocator): ByteArray? {
    val arrayBuffers = mutableListOf<ByteArray>()
    var currentBuffer = ByteArray(BUFFER_SIZE)
    var currentBufferIndex = 0

    val singleBytePtr = allocator.allocate(1)
    val ioVecPtr = allocator.allocate(8)
    (ioVecPtr + 0).storeInt(singleBytePtr.address.toInt())
    (ioVecPtr + 4).storeInt(1)

    val rp0 = allocator.allocate(4)

    var crInCurrentBuffer = false
    while (true) {
        val ret = wasiRawFdRead(
            descriptor = STDIN,
            gatherPtr = ioVecPtr.address.toInt(),
            gatherSize = 1,
            errorPtr = rp0.address.toInt()
        )
        if (ret != 0) {
            throw WasiError(WasiErrorCode.entries[ret])
        }
        val readSize = rp0.loadInt()
        check(readSize == 0 || readSize == 1) { "Unexpected WASI result" }
        if (readSize == 0 && currentBufferIndex == 0 && arrayBuffers.isEmpty()) return null

        val nextByte = singleBytePtr.loadByte()
        if (readSize == 0 || nextByte == LF) {
            if (crInCurrentBuffer) {
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
            return result
        }

        if (currentBufferIndex >= BUFFER_SIZE) {
            arrayBuffers.add(currentBuffer)
            currentBuffer = ByteArray(BUFFER_SIZE)
            currentBufferIndex = 0
        }

        currentBuffer[currentBufferIndex] = nextByte
        crInCurrentBuffer = nextByte == CR
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
public actual fun readlnOrNull(): String? = withScopedMemoryAllocator { allocator ->
    wasiWaitUntilUserInputImpl(allocator)
    wasiReadLineImpl(allocator)?.decodeToString()
}
