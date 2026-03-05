/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.wasm.unsafe

import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import kotlin.internal.DoNotInlineOnFirstStage
import kotlin.wasm.internal.wasm_memory_copy
import kotlin.wasm.internal.wasm_memory_grow
import kotlin.wasm.internal.wasm_memory_size

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
@DoNotInlineOnFirstStage
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
    check(reallocAllocator == null) {
        "Can't create new allocators while realloc-allocated memory is not freed"
    }
    val allocator = currentAllocator?.createChild() ?:
        ScopedMemoryAllocator(0, parent = null)
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
        check(result >= 0 && result % align == 0) { "result must be >= 0 and 8-byte aligned" }

        if (Int.MAX_VALUE - availableAddress < size) {
            error("Out of linear memory. All available address space (2gb) is used.")
        }

        availableAddress = result + size

        val currentMaxSize = wasm_memory_size() * WASM_PAGE_SIZE_IN_BYTES
        if (availableAddress >= currentMaxSize) {

            val numPagesToGrow =
                (availableAddress - currentMaxSize) / WASM_PAGE_SIZE_IN_BYTES + 2

            if (wasm_memory_grow(numPagesToGrow) == -1) {
                error("Out of linear memory. memory.grow returned -1")
            }
        }

        check(availableAddress < wasm_memory_size() * WASM_PAGE_SIZE_IN_BYTES)

        return Pointer(result.toUInt())
    }

    @PublishedApi
    internal fun createChild(): ScopedMemoryAllocator {
        val child = ScopedMemoryAllocator(availableAddress, parent = this)
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

@OptIn(UnsafeWasmMemoryApi::class)
private var reallocAllocator: ScopedMemoryAllocator? = null

private var lastReallocAllocatedAddress: Int? = null

/**
 * WebAssembly Component Model Canonical ABI realloc implementation.
 * This function is intended to be exported to a Component Model and must not be called directly.
 * Memory allocated by this function must be freed
 * by calling [freeAllComponentModelReallocAllocatedMemory] before calling any [withScopedMemoryAllocator].
 */
@OptIn(UnsafeWasmMemoryApi::class)
@ComponentModelInternalApi
public fun componentModelRealloc(
    originalPtr: Int,
    originalSize: Int,
    newSize: Int
): Int {
    // The first call to realloc creates a new allocator.
    // Later calls to realloc reuse the previous allocator until freeAllReallocAllocatedMemory is called.
    if (reallocAllocator == null) {
        reallocAllocator = createAllocatorInTheNewScope()
    }
    val allocator = reallocAllocator!!

    val result = when {
        // Common case of allocating fresh memory when the original size is zero.
        originalSize == 0 -> {
            allocator.allocate(newSize).address.toInt()
        }
        // Growing allocation on top of the bump allocator stack by allocating the size difference and returning the original address.
        lastReallocAllocatedAddress == originalPtr -> {
            val _ = allocator.allocate(newSize - originalSize)
            originalPtr
        }
        // Allocation "in the middle" of bump allocator can't be grown in size in place.
        // Allocating fresh memory and copying the data.
        else -> {
            val newPtr = allocator.allocate(newSize).address.toInt()
            wasm_memory_copy(newPtr, originalPtr, originalSize)
            newPtr
        }
    }
    lastReallocAllocatedAddress = result
    return result
}

/**
 *  Frees memory allocated by all previous calls of [componentModelRealloc]. 
 */
@OptIn(UnsafeWasmMemoryApi::class)
@ComponentModelInternalApi
public fun freeAllComponentModelReallocAllocatedMemory() {
    if (reallocAllocator != null) {
        reallocAllocator!!.destroy()
        currentAllocator = reallocAllocator!!.parent
        reallocAllocator = null
        lastReallocAllocatedAddress = null
    }
}
