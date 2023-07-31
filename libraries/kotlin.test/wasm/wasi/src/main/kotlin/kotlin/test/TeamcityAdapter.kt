/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.test

import kotlin.wasm.WasmImport
import kotlin.wasm.unsafe.*

/**
 * Read command-line argument data. The size of the array should match that returned by
 * `args_sizes_get`. Each argument is expected to be `\0` terminated.
 */
@WasmImport("wasi_snapshot_preview1", "args_get")
private external fun wasiRawArgsGet(
    argvPtr: Int,
    argvBuf: Int,
): Int


/** Return command-line argument data sizes. */
@WasmImport("wasi_snapshot_preview1", "args_sizes_get")
private external fun wasiRawArgsSizesGet(
    argumentNumberPtr: Int,
    argumentStringSizePtr: Int,
): Int

@OptIn(UnsafeWasmMemoryApi::class)
internal fun getArguments(): List<String> = withScopedMemoryAllocator { allocator ->
    val numberOfArgumentsPtr = allocator.allocate(4)
    val sizeOfArgumentStringPtr = allocator.allocate(4)
    val argNumRes = wasiRawArgsSizesGet(
        argumentNumberPtr = numberOfArgumentsPtr.address.toInt(),
        argumentStringSizePtr = sizeOfArgumentStringPtr.address.toInt()
    )
    if (argNumRes != 0) {
        throw IllegalStateException("Wasi error code $argNumRes")
    }

    val argumentNumber = numberOfArgumentsPtr.loadInt()
    if (argumentNumber <= 2) return emptyList()

    val argumentStringSize = sizeOfArgumentStringPtr.loadInt()
    val stringBufferPtr = allocator.allocate(argumentStringSize)

    val argvPtr = allocator.allocate(argumentNumber * Int.SIZE_BYTES)

    val argRes = wasiRawArgsGet(argvPtr.address.toInt(), stringBufferPtr.address.toInt())
    if (argRes != 0) {
        throw IllegalStateException("Wasi error code $argNumRes")
    }

    val startAddress = (argvPtr + 2 * Int.SIZE_BYTES).loadInt().toUInt()
    val endAddress = stringBufferPtr.address + argumentStringSize.toUInt()
    decodeStrings(argumentStringSize, startAddress, endAddress)
}

@OptIn(UnsafeWasmMemoryApi::class)
private fun decodeStrings(arraySize: Int, startAddress: UInt, endAddress: UInt): List<String> {
    val buffer = ByteArray(arraySize)
    val result = mutableListOf<String>()

    var currentPtr = Pointer(startAddress)
    var currentIndex = 0
    while (currentPtr.address < endAddress) {
        val byte = currentPtr.loadByte()
        if (byte.toInt() == 0) {
            result.add(buffer.decodeToString(0, currentIndex))
            currentIndex = 0
        } else {
            buffer[currentIndex] = byte
            currentIndex++
        }
        currentPtr += 1
    }
    return result
}