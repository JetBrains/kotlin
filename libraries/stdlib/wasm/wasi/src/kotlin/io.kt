/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.io

import kotlin.wasm.WasiError
import kotlin.wasm.WasiErrorCode
import kotlin.wasm.WasmImport
import kotlin.wasm.unsafe.MemoryAllocator
import kotlin.wasm.unsafe.withScopedMemoryAllocator

private const val STDOUT = 1
private const val STDERR = 2

/**
 * Write to a file descriptor. Note: This is similar to `writev` in POSIX.
 */
@WasmImport("wasi_snapshot_preview1", "fd_write")
private external fun wasiRawFdWrite(descriptor: Int, scatterPtr: Int, scatterSize: Int, errorPtr: Int): Int

internal fun wasiPrintImpl(
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

internal fun printError(error: String?) {
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

@SinceKotlin("1.6")
public actual fun readln(): String = TODO("wasi")

@SinceKotlin("1.6")
public actual fun readlnOrNull(): String? = TODO("wasi")