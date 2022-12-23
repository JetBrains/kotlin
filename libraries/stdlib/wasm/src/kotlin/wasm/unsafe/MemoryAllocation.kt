/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.wasm.unsafe

import kotlin.wasm.internal.WasmOp
import kotlin.wasm.internal.implementedAsIntrinsic
import kotlin.wasm.internal.unsafeGetScratchRawMemory

/**
 * WebAssembly linear memory allocator.
 */
@UnsafeWasmApi
public abstract class MemoryAllocator {
    /**
     * Allocate a block of uninitialized linear memory of given [size] in bytes.
     *
     * @return an address of allocated memory. It is guaranteed to be a multiple of 8.
     */
    public abstract fun allocate(size: Int): Pointer
}

/**
 * Run a [block] of code, providing it a temporary [MemoryAllocator] as an argument, and return its result.
 *
 * Free all memory allocated with provided allocator after running the [block].
 *
 * This function is intened to facilitate the exchange of values with outside world through linear memory.
 * For example:
 *
 *    val buffer_size = ...
 *    withScopedMemoryAllocator { allocator ->
 *        val buffer_address = allocator.allocate(buffer_size)
 *        importedWasmFunctionThatWritesToBuffer(buffer_address, buffer_size)
 *        return readDataFromBufferIntoManagedKotlinMemory(buffer_address, buffer_size)
 *    }
 *
 * WARNING! Addresses leaked outside of [block] scope become invalid and can be overridden.
 *
 * WARNING! Nested calls to [withScopedMemoryAllocator] will throw [IllegalStateException].
 *          The standard library may use this allocator for JS interop bindings. For example,
 *          it may use it to copy strings.
 *
 * WARNING! Accessing allocator outside of the [block] scope will throw [IllegalStateException].
 */
@UnsafeWasmApi
public inline fun <T> withScopedMemoryAllocator(
    block: (allocator: MemoryAllocator) -> T
): T {
    check(!inScopedMemoryAllocatorBlock) { "Calls to withScopedMemoryAllocator can't be nested" }
    inScopedMemoryAllocatorBlock = true
    val allocator = ScopedMemoryAllocator()
    val result = try {
        block(allocator)
    } finally {
        inScopedMemoryAllocatorBlock = false
        allocator.destroy()
    }
    return result
}


@PublishedApi
@UnsafeWasmApi
internal var inScopedMemoryAllocatorBlock: Boolean = false

@PublishedApi
@UnsafeWasmApi
internal class ScopedMemoryAllocator : MemoryAllocator() {
    private var destroyed = false
    private var availableAddress: ULong = unsafeGetScratchRawMemory().toULong()

    override fun allocate(size: Int): Pointer {
        check(!destroyed) { "ScopedMemoryAllocator is destroyed when out of scope" }

        // Pad available address to align it to 8
        // 8 is a max alignment number currently needed for Wasm component model canonical ABI
        val align = 8uL
        val result = (availableAddress + align - 1uL) and (align - 1uL).inv()
        check(result % 8uL == 0uL)

        availableAddress = result + size.toULong()

        if (availableAddress > UInt.MAX_VALUE.toULong()) {
            error("Out of linear memory. All available address space (4gb) is used.")
        }

        val currentMaxSize = wasmMemorySize().toULong() * WASM_PAGE_SIZE_IN_BYTES.toULong()
        if (availableAddress >= currentMaxSize) {

            val numPagesToGrow =
                (availableAddress - currentMaxSize) / WASM_PAGE_SIZE_IN_BYTES.toULong() + 2uL

            if (wasmMemoryGrow(numPagesToGrow.toInt()) == -1) {
                error("Out of linear memory. memory.grow returned -1")
            }
        }

        check(availableAddress < wasmMemorySize().toULong() * WASM_PAGE_SIZE_IN_BYTES.toULong())

        return result.toInt()
    }

    fun destroy() {
        destroyed = true
    }
}

private const val WASM_PAGE_SIZE_IN_BYTES = 65_536  // 64 KiB

/**
 * Current linear memory size in pages
 */
@WasmOp(WasmOp.MEMORY_SIZE)
internal fun wasmMemorySize(): Int =
    implementedAsIntrinsic

/**
 * Grow memory by a given delta (in pages).
 * Return the previous size, or -1 if enough memory cannot be allocated.
 */
@WasmOp(WasmOp.MEMORY_GROW)
internal fun wasmMemoryGrow(delta: Int): Int =
    implementedAsIntrinsic