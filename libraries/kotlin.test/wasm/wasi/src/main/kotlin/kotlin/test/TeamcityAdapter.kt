/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.test

import kotlin.wasm.WasmImport
import kotlin.wasm.ExperimentalWasmInterop
import kotlin.wasm.unsafe.*

/**
 * `wasi:cli/environment.get-arguments`, i.e. `func() -> list<string>` in the Canonical ABI: the result is written into
 * a return area (pointer, length), and each element is a (pointer, length) pair of UTF-8 bytes. 
 *
 * The memory for the list itself is automatically allocated by cabi_realloc, and needs to be freed manually
 *
 * NOTE: This cannot simply be done by implementing commandline arguments for the WASI subtarget in general, as those wouldn't easily be accessible from this generic `getArguments` interface.
 */
@ExperimentalWasmInterop
@WasmImport("wasi:cli/environment@0.2.12", "get-arguments")
private external fun wasiCliGetArguments(returnArea: Int)

private const val POINTER_AND_LENGTH_SIZE = 2 * Int.SIZE_BYTES

@OptIn(ComponentModelInternalApi::class, UnsafeWasmMemoryApi::class, ExperimentalWasmInterop::class)
internal actual fun getArguments(): List<String> = withScopedMemoryAllocator { allocator ->
    val returnArea = allocator.allocate(POINTER_AND_LENGTH_SIZE)
    wasiCliGetArguments(returnArea.address.toInt())

    val listElementsPtr = returnArea.loadInt()
    val listSize = (returnArea + Int.SIZE_BYTES).loadInt()

    // the first argument is the program name, everything after it is a test runner argument, so drop(1) after
    List(listSize) { index ->
        val element = Pointer((listElementsPtr + index * POINTER_AND_LENGTH_SIZE).toUInt())
        val stringPtr = element.loadInt()
        val stringSize = (element + Int.SIZE_BYTES).loadInt()
        loadString(Pointer(stringPtr.toUInt()), stringSize).also{
            // free the string memory allocated by cabi_realloc by the host
            componentModelRealloc(stringPtr, /* each element (string char) is 1B big */ stringSize, /* newSize = 0 means 'free it' */ 0)
        }
    }.drop(1).also {
        // free the list memory allocated by cabi_realloc by the host
        componentModelRealloc(listElementsPtr, listSize * POINTER_AND_LENGTH_SIZE, /* free */ 0);
    }

    // the return area is freed automatically by withScopedMmeoryALlocator
}

@OptIn(UnsafeWasmMemoryApi::class)
private fun loadString(address: Pointer, length: Int): String {
    val bytes = ByteArray(length)
    for (index in 0 until length) {
        bytes[index] = (address + index).loadByte()
    }
    return bytes.decodeToString()
}

