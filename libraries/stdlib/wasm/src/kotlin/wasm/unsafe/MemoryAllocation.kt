/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.wasm.unsafe

import kotlin.wasm.internal.WasmOp
import kotlin.wasm.internal.implementedAsIntrinsic
import kotlin.wasm.internal.unsafeGetScratchRawMemory
import kotlin.contracts.*

/**
 * WebAssembly linear memory allocator.
 */
@UnsafeWasmMemoryApi
public abstract class MemoryAllocator {
    /**
     * Allocates a block of uninitialized linear memory of the given [size] in bytes.
     *
     * @return an address of allocated memory. It is guaranteed to be a multiple of 8.
     */
    public abstract fun allocate(size: Int): Pointer
}

/**
 * Runs the [block] of code, providing it a temporary [MemoryAllocator] as an argument, and returns the result of this block.
 *
 * Frees all memory allocated with the provided allocator after running the [block].
 *
 * This function is intened to facilitate the exchange of values with outside world through linear memory.
 * For example:
 *
 * ```
 * val buffer_size = ...
 * withScopedMemoryAllocator { allocator ->
 *     val buffer_address = allocator.allocate(buffer_size)
 *     importedWasmFunctionThatWritesToBuffer(buffer_address, buffer_size)
 *     return readDataFromBufferIntoManagedKotlinMemory(buffer_address, buffer_size)
 * }
 * ```
 *
 * WARNING! Addresses allocated inside the [block] function become invalid after exiting the function.
 *
 * WARNING! A nested call to [withScopedMemoryAllocator] will temporarily disable the allocator from the outer scope
 *   for the duration of the call. Calling [MemoryAllocator.allocate] on a disabled allocator
 *   will throw [IllegalStateException].
 *
 * WARNING! Accessing the allocator outside of the [block] scope will throw [IllegalStateException].
 */
@UnsafeWasmMemoryApi
public inline fun <T> withScopedMemoryAllocator(
    block: (allocator: MemoryAllocator) -> T
): T {
    contract { callsInPlace(block, InvocationKind.EXACTLY_ONCE) }
    val allocator = createAllocatorInTheNewScope()
    val result = try {
        block(allocator)
    } finally {
        allocator.destroy()
        currentAllocator = allocator.parent
    }
    return result
}

@PublishedApi
@UnsafeWasmMemoryApi
internal fun createAllocatorInTheNewScope(): ScopedMemoryAllocator {
    val allocator = currentAllocator?.createChild() ?:
        ScopedMemoryAllocator(unsafeGetScratchRawMemory(), parent = null)
    currentAllocator = allocator
    return allocator
}


@PublishedApi
@UnsafeWasmMemoryApi
internal var currentAllocator: ScopedMemoryAllocator? = null

// TODO(KT-58041): Consider switching back to using ULong
@PublishedApi
@UnsafeWasmMemoryApi
internal class ScopedMemoryAllocator(
    startAddress: Int,
    // Allocator from parent scope or null for top-level scope.
    @PublishedApi
    internal var parent: ScopedMemoryAllocator?,
) : MemoryAllocator() {
    // true if allocator is out of scope
    private var destroyed = false

    // true if child allocator is active
    private var suspended = false

    // all memory is available starting from this address
    private var availableAddress = startAddress

    override fun allocate(size: Int): Pointer {
        check(!destroyed) { "ScopedMemoryAllocator is destroyed when out of scope" }
        check(!suspended) { "ScopedMemoryAllocator is suspended when nested allocators are used" }

        // Pad available address to align it to 8
        // 8 is a max alignment number currently needed for Wasm component model canonical ABI
        val align = 8
        val result = (availableAddress + align - 1) and (align - 1).inv()
        check(result > 0 && result % align == 0) { "result must be > 0 and 8-byte aligned" }

        if (Int.MAX_VALUE - availableAddress < size) {
            error("Out of linear memory. All available address space (2gb) is used.")
        }

        availableAddress = result + size

        val currentMaxSize = wasmMemorySize() * WASM_PAGE_SIZE_IN_BYTES
        if (availableAddress >= currentMaxSize) {

            val numPagesToGrow =
                (availableAddress - currentMaxSize) / WASM_PAGE_SIZE_IN_BYTES + 2

            if (wasmMemoryGrow(numPagesToGrow) == -1) {
                error("Out of linear memory. memory.grow returned -1")
            }
        }

        check(availableAddress < wasmMemorySize() * WASM_PAGE_SIZE_IN_BYTES)

        return Pointer(result.toUInt())
    }

    @PublishedApi
    internal fun createChild(): ScopedMemoryAllocator {
        val child = ScopedMemoryAllocator(availableAddress.toInt(), parent = this)
        suspended = true
        return child
    }

    @PublishedApi
    internal fun destroy() {
        destroyed = true
        parent?.suspended = false
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
@Suppress("UNUSED_PARAMETER")
@WasmOp(WasmOp.MEMORY_GROW)
internal fun wasmMemoryGrow(delta: Int): Int =
    implementedAsIntrinsic