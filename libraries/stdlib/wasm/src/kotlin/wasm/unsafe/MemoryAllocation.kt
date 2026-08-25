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
 * This function is intended to facilitate the exchange of values with the outside world through linear memory.
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
    block: (allocator: MemoryAllocator) -> T,
): T {
    // TODO get rid of currentAllocator thing
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
internal fun createAllocatorInTheNewScope(): ArenaLikeAllocator {
    val allocator = ArenaLikeAllocator(0, parent = null)
    currentAllocator = allocator
    return allocator
}

@UnsafeWasmMemoryApi
private class MemorySlot(val ptr: Pointer, val size: UInt) {
    fun tryMerge(b: MemorySlot): MemorySlot? {
        val a = this

        val aAddr = a.ptr.address
        val bAddr = b.ptr.address

        assert(bAddr != aAddr) { "Slots cannot describe memory starting at the exact same address" }


        // basically: check for range overlap, but perfect adjacency counts as overlap
        // one-sided as in "only check whether one slot extends into/next to the other", not the other way around
        fun oneSidedMerge(x: MemorySlot, y: MemorySlot): MemorySlot? {
            val xAddr = x.ptr.address
            val yAddr = y.ptr.address

            // x starts before y
            if (xAddr < yAddr) {
                // if x extends into (or just next to) y
                if (xAddr + x.size >= yAddr) {
                    // the combined allocation end is one of the previous ends, just whichever is larger
                    val newEndAddr = maxOf(xAddr + x.size, yAddr + y.size)
                    return MemorySlot(x.ptr, newEndAddr - x.ptr.address)
                }
            }
            return null
        }

        return listOfNotNull(
            oneSidedMerge(a, b),
            oneSidedMerge(b, a)
        ).firstOrNull()
    }
}

// NOTE: design choice for now: store all the info here (i.e., in WasmGC structs), instead of trying to be clever and use headers in linear memory or similar.
// TODO think about this again, but I don't see a reason to use headers if we have other memory available anyway
@UnsafeWasmMemoryApi
private object FreeList {
    val alignment = 8u

    // TODO right now, this is the default list implementation, which I assume is an array list. That's not really optimal, because it requires copying around stuff when the number of free slots change, and we can't make use of O(1) element access
    val list = mutableListOf<MemorySlot>(
        MemorySlot(Pointer(0u), ((1u shl 31) - 1u))
    )

    // NOTE: freeing is when things are merged back together

    @PublishedApi
    internal fun free(slot: MemorySlot): Unit = TODO("merge stuff")

    @PublishedApi
    internal fun allocate(size: UInt): MemorySlot {
        // round up the size to a multiple of 8, so that all addresses are always guaranteed to be aligned to at least 8
        // by adding 7, we're guaranteed to:
        // - if size mod 8 == 0: NOT cross the divisible-by-8 boundary
        // - if size mod 8 != 0: cross the divisible-by-8 boundary exactly once.
        // so after adding 7, just shave off the lower bits under 7
        val roundedSize = (size + (alignment - 1u)) and (alignment - 1u).inv()
        //    equation for now: (size + 7u              )  &   0xFFFFFFF8u

        val slotIndex = list.indexOfFirst { it.size >= roundedSize }
        if (slotIndex == -1)
            throw OutOfMemoryError("Out of linear memory. All available address space (2gb) is used.")

        val slot = list[slotIndex]

        if (slot.size == roundedSize) {
            list.removeAt(slotIndex)
            return slot
        } else {
            // in this case, split the slot into 2, return the left part, and reinsert the right part
            val allocatedSlot = MemorySlot(slot.ptr, roundedSize)
            val reinsertedSlot = MemorySlot(slot.ptr + roundedSize, slot.size - roundedSize)

            list[slotIndex] = reinsertedSlot
            return allocatedSlot
        }
    }
}

@PublishedApi
@UnsafeWasmMemoryApi
internal var currentAllocator: ArenaLikeAllocator? = null

// TODO(KT-58041): Consider switching back to using ULong
// TODO can we safely rename an @PublishedApi class?
@PublishedApi
@UnsafeWasmMemoryApi
internal class ArenaLikeAllocator(
    // TODO can we remove these? they're useless now
    startAddress: Int,
    // Allocator from parent scope or null for top-level scope.
    @PublishedApi
    internal var parent: ArenaLikeAllocator?,
) : MemoryAllocator() {
    private val allocations = mutableListOf<MemorySlot>()

    override fun allocate(size: Int): Pointer {
        // TODO go back to UInt for this too
        check(size >= 0) { "size must be >= 0" }

        // Pad available address to align it to 8
        // 8 is a max alignment number currently needed for Wasm component model canonical ABI
        val result = FreeList.allocate(size.toUInt())
        check(result.ptr.address % 8u == 0u) { "result must be 8-byte aligned" }

        val firstInvalidAddress = wasm_memory_size().toUInt() * WASM_PAGE_SIZE_IN_BYTES.toUInt()
        val endAddressExclusive = result.ptr.address.toULong() + result.size
        if (endAddressExclusive >= firstInvalidAddress) {

            val numPagesToGrow =
                (endAddressExclusive - firstInvalidAddress) / WASM_PAGE_SIZE_IN_BYTES.toUInt() + 2u

            if (wasm_memory_grow(numPagesToGrow.toInt()) == -1) {
                error("Out of linear memory. memory.grow returned -1")
            }
        }

        check(endAddressExclusive < wasm_memory_size().toUInt() * WASM_PAGE_SIZE_IN_BYTES.toUInt())

        // track this allocation so they can all be freed on destroy()
        allocations.add(result)

        // TODO returning the exact object is not a problem because of immutability, right?
        return result.ptr
    }

    @PublishedApi
    internal fun createChild(): ArenaLikeAllocator {
        return createAllocatorInTheNewScope()
    }

    @PublishedApi
    internal fun destroy() {
        // TODO once we figure out the cabi realloc frees, also actually free this
        /*
        for (allocation in allocations) {
            FreeList.free(allocation)
        }
        allocations.clear()
         */
    }
}

private const val WASM_PAGE_SIZE_IN_BYTES = 65_536  // 64 KiB

@OptIn(UnsafeWasmMemoryApi::class)
private var reallocAllocator: ArenaLikeAllocator? = null

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
    newSize: Int,
): Int {
    // The first call to realloc creates a new allocator.
    // Later calls always reuse the realloc allocator, it does not die anymore
    if (reallocAllocator == null) {
        reallocAllocator = createAllocatorInTheNewScope()
    }
    val allocator = reallocAllocator!!

    val newAllocation = allocator.allocate(newSize)
    if (originalSize < newSize && originalPtr + originalSize == newAllocation.address.toInt()) {
        // in that case, don't need to copy because we just grew at the same point
        return originalPtr
    }

    // otherwise, we'll have to copy the old data, if the old size wasn't zero
    if (originalSize > 0)
        wasm_memory_copy(newAllocation.address.toInt(), originalPtr, minOf(originalSize, newSize))

    return newAllocation.address.toInt()
}

/**
 *  Frees memory allocated by all previous calls of [componentModelRealloc]. 
 */
@OptIn(UnsafeWasmMemoryApi::class)
@ComponentModelInternalApi
public fun freeAllComponentModelReallocAllocatedMemory() {
    // empty for now, will be unused soon
}
