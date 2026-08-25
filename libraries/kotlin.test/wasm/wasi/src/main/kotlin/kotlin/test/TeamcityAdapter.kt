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
 * a return area as a (pointer, length) pair, and each element is a (pointer, length) pair of UTF-8 bytes. The memory
 * for the list itself is allocated by the guest's exported `cabi_realloc`.
 *
 * Deliberately not the preview1 `args_get`/`args_sizes_get`: those would force every test binary to be componentized
 * with a `wasi_snapshot_preview1` adapter (see KT-87723), even though the standard library is WASI 0.2 native.
 */
@ExperimentalWasmInterop
@WasmImport("wasi:cli/environment@0.2.12", "get-arguments")
private external fun wasiCliGetArguments(returnArea: Int)

private const val POINTER_AND_LENGTH_SIZE = 2 * Int.SIZE_BYTES

@OptIn(UnsafeWasmMemoryApi::class, ExperimentalWasmInterop::class)
internal actual fun getArguments(): List<String> = withScopedMemoryAllocator { allocator ->
    val returnArea = allocator.allocate(POINTER_AND_LENGTH_SIZE)
    wasiCliGetArguments(returnArea.address.toInt())

    val elementsPtr = returnArea.loadInt()
    val size = (returnArea + Int.SIZE_BYTES).loadInt()

    // the first argument is the program name, everything after it is a test runner argument
    List(size) { index ->
        val element = Pointer((elementsPtr + index * POINTER_AND_LENGTH_SIZE).toUInt())
        loadString(Pointer(element.loadInt().toUInt()), (element + Int.SIZE_BYTES).loadInt())
    }.drop(1)
}

@OptIn(UnsafeWasmMemoryApi::class)
private fun loadString(address: Pointer, length: Int): String {
    val bytes = ByteArray(length)
    for (index in 0 until length) {
        bytes[index] = (address + index).loadByte()
    }
    return bytes.decodeToString()
}
