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
private data class MemorySlot(val ptr: Pointer, val size: UInt) {
    // TODO in the usages, this is only ever used where we know the direction of the only possible successful one-way merge. So could optimize it based on that
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

/**
 * Returns size but possibly lengthened to align to an implementation-defined alignment.
 *
 * This means it represents the actual size of any allocation made with the size parameter.
 *
 * The alignment is currently 8, as it's currently the maximum needed for the Wasm component model canonical ABI.
 */
private fun realAllocationSize(size: UInt): UInt {
    val alignment = 8u

    // round up the size to a multiple of 8, so that all addresses are always guaranteed to be aligned to at least 8
    // by adding 7, we're guaranteed to:
    // - if size mod 8 == 0: NOT cross the divisible-by-8 boundary
    // - if size mod 8 != 0: cross the divisible-by-8 boundary exactly once.
    // so after adding 7, just shave off the lower bits under 7
    return (size + (alignment - 1u)) and (alignment - 1u).inv()
    //     (size + 7u              )  &   0xFFFFFFF8u
}


// NOTES:
// - design choice for now: store all the info here (i.e., in WasmGC structs), instead of trying to be clever and use headers in linear memory or similar.
// - NOT thread-safe, would need synchronization if not used in a single-threaded environment
// - NOT reentrant, i.e., cannot call any member functions of this, from within any member functions of this
// TODO think about this again, but I don't see a reason to use headers if we have other memory available anyway
@UnsafeWasmMemoryApi
private object FreeList {
    // TODO remove
    fun debugDump(): String = buildString {
        appendLine("FreeList:")
        for (it in list) {
            appendLine("  ${it.ptr.address} - ${it.ptr.address + it.size}")
        }
    }

    // TODO right now, this is the default list implementation, which I assume is an array list. That's not really optimal, because it requires copying around stuff when the number of free slots change, and we can't make use of O(1) element access
    val list = mutableListOf<MemorySlot>(
        MemorySlot(Pointer(0u), ((1u shl 31) - 1u))
    )

    /**
     * TODO(REVIEW) can we get rid of this / make it test only? Should we keep it even in production?
     *
     * Make sure to not call call free / alloc from anywhere inside free / alloc
     */
    private var isAlreadyOperating = false

    // NOTE: freeing is when things are merged back together

    @PublishedApi
    internal fun free(allocatedSlot: MemorySlot): Unit {
        check(!isAlreadyOperating) { "Cannot call free from within the allocator" }
        isAlreadyOperating = true
        try {
            check(allocatedSlot.size == realAllocationSize(allocatedSlot.size)) { "Slot to free clearly does not originate from allocated slot: alignment is wrong" }

            // need to find the slots that this lies in between, in terms of start address
            // NOTE: we assume the allocatedSlot does not overlap with anything in the free list, that wouldn't make sense, by definition, allocatedSlot is not free
            val minusInsertionPointMinusOne = list.binarySearch {
                // because we assume it can't overlap, we know that size is irrelevant here: we'll get the index of the insertion point from this function, and inserting there will not lead to overlap
                (it.ptr.address.toLong() - allocatedSlot.ptr.address.toLong()).toInt()
            }

            require(minusInsertionPointMinusOne != 0) { "Double-free: slot to free can't already be in the free list; slot to free: $allocatedSlot; " + FreeList.debugDump() }

            // convert back to actual insertion point
            val insertionPointIndex = -(minusInsertionPointMinusOne + 1)

            // before we insert, try to merge
            val leftElement = list.getOrNull(insertionPointIndex - 1)
            val rightElement = list.getOrNull(insertionPointIndex)
            check(
                leftElement?.ptr?.address?.let { it < allocatedSlot.ptr.address } ?: true &&
                        rightElement?.ptr?.address?.let { allocatedSlot.ptr.address < it } ?: true
            ) { "Binary search has gone wrong" }

            // once we start merging anything, the left and right slots might become adjacent, and need to be merged themselves
            fun tryMergeLeftAndRight() {
                // need to access left and right again here, as they can change during the function
                val leftElement = list.getOrNull(insertionPointIndex - 1)
                val rightElement = list.getOrNull(insertionPointIndex)

                if (leftElement == null || rightElement == null)
                    return

                val successfulMerge = leftElement.tryMerge(rightElement)
                if (successfulMerge != null) {
                    list[insertionPointIndex - 1] = successfulMerge
                    list.removeAt(insertionPointIndex)
                    return
                }
            }

            val successfulMergeLeft = leftElement?.tryMerge(allocatedSlot)
            if (successfulMergeLeft != null) {
                list[insertionPointIndex - 1] = successfulMergeLeft
                // now that we merged, left and right might be adjacent
                tryMergeLeftAndRight()
                return
            }

            val successfulMergeRight = rightElement?.tryMerge(allocatedSlot)
            if (successfulMergeRight != null) {
                list[insertionPointIndex] = successfulMergeRight
                tryMergeLeftAndRight()
                return
            }

            // otherwise, we couldn't merge with either side, so by definition there's no overlap, and we just need to insert a new slot
            list.add(insertionPointIndex, allocatedSlot)
        } finally {
            isAlreadyOperating = false
        }
    }

    @PublishedApi
    internal fun allocate(size: UInt): MemorySlot {
        check(!isAlreadyOperating) { "Cannot call free from within the allocator" }
        isAlreadyOperating = true
        try {
            val alignedSize = realAllocationSize(size)

            val slotIndex = list.indexOfFirst { it.size >= alignedSize }
            if (slotIndex == -1)
                throw OutOfMemoryError("Out of linear memory. All available address space (2gb) is used.")

            val slot = list[slotIndex]

            if (slot.size == alignedSize) {
                list.removeAt(slotIndex)
                return slot
            } else {
                // in this case, split the slot into 2, return the left part, and reinsert the right part
                val allocatedSlot = MemorySlot(slot.ptr, alignedSize)
                val reinsertedSlot = MemorySlot(slot.ptr + alignedSize, slot.size - alignedSize)

                list[slotIndex] = reinsertedSlot
                return allocatedSlot
            }
        } finally {
            isAlreadyOperating = false
        }
    }
}

// TODO IMPORTANT ======================
//      In the end, the cleanest will probably be to have the scoped allocator still exist, but so ave the arena like allocator as a private class here, and then the scoped allocator just wraps it.
// TODO IMPORTANT ======================

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

    // NOTE: we don't expose a free() function directly, to a) make it harder to write use-after-free's, and b) not expose MemorySlot / FreeList beyond this file

    @PublishedApi
    internal fun destroy() {
        // TODO once we figure out the cabi realloc frees, also actually free this
        //      Wait, we can just already free this no? What speaks against that?
        for (allocation in allocations) {
            FreeList.free(allocation)
        }
        allocations.clear()
    }
}

private const val WASM_PAGE_SIZE_IN_BYTES = 65_536  // 64 KiB

@OptIn(UnsafeWasmMemoryApi::class)
private var reallocAllocator: ArenaLikeAllocator? = null

/**
 * WebAssembly Component Model Canonical ABI realloc implementation.
 * This function is intended to be exported for Component Model support and must not be called directly.
 *
 * Memory allocated by this function must be freed by calling this function again,
 * with the original pointer, the original size, and new size 0.
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

    // to address the correct slot, we must extend the original size to be aligned, as that will be the internal size of the slot
    val originalAllocationSize = realAllocationSize(originalSize.toUInt())

    if (newSize == 0) {

        // TODO this is an easy way to get the program to trap, if it's misused. Any possible guardrails against this?
        FreeList.free(MemorySlot(Pointer(originalPtr.toUInt()), originalAllocationSize))
        // TODO figure out a fitting return value here.
        return 0
    }

    val newAllocationSize = realAllocationSize(newSize.toUInt())

    // cases:
    // 1. size doesn't change
    // 2. allocation shrinks
    // 3. allocation grows
    //   3a. fresh allocation (original size was 0)
    //       NOTE: this would technically be handled by case 3b, but it's simpler to handle it separately
    //   3b. allocation grows in place
    //   3c. allocation grows elsewhere, needs copy

    if (newAllocationSize == originalAllocationSize) // case 1
        return originalPtr

    // case 2: shrinking, i.e., the new size is smaller than the old size: nothing to do except free a portion
    if (newAllocationSize < originalAllocationSize) {
        // NOTE: because we're only subtracting aligned sizes, the result will still be aligned
        FreeList.free(MemorySlot(Pointer(originalPtr.toUInt() + newAllocationSize), originalAllocationSize - newAllocationSize))
        return originalPtr
    }

    // case 3: growing, i.e., we need to do some actual allocation
    val newAllocation = allocator.allocate(newSize)

    // case 3a: the original size was 0, we're done
    if (originalSize == 0)
        return newAllocation.address.toInt()

    // case 3b: we can grow the allocation in place
    if (originalAllocationSize <= newAllocationSize && originalPtr.toUInt() + originalAllocationSize == newAllocation.address) {
        // in that case, don't need to copy data from the old allocation because we just grew at the same point
        // BUT: Because we grew, we're actually reusing the original allocation with its original aligned size.
        //      But at this moment, we just have one big allocation with size originalSizeAligned + newSizeAligned.
        //      So free the difference.
        val startOfOverallocatedMemory = originalPtr.toUInt() + newAllocationSize
        val overallocatedSize = originalAllocationSize // we allocated as if we didn't have the original allocation
        FreeList.free(MemorySlot(Pointer(startOfOverallocatedMemory), overallocatedSize))
        // NOTE: allocating and then freeing again might seem overcomplicated; the obvious alternative would be to allocate twice in a row instead. The reason not to allocate twice is as follows:
        //       if the allocator ever changes, and stops giving out contiguous memory, this code path, as it stands, will simply stop being used, and nothing will break.
        //       While the allocation does occur contiguously, the free is also contiguous and doesn't perform any complex logic, because all that changes is the start address of the free list block that we're allocating from, the list itself is not modified.
        //
        //       If we instead allocated twice, in case we can't grow the original allocation in place, we're implicitly relying on being able to perform the second allocation in place. This isn't always true, and would create further complications in these cases, by having to free the "failed" allocation first. Thus, overallocating plus freeing is safer than allocating incrementally.
        //       Conversely, this implementation suffers from sometimes not being able to grow an allocation in place, when the initial overallocatedSize is too large. However, this only results in an additional copy, instead of a semantics change.

        return originalPtr
    }

    // case 3c: we now know the allocation grew (and the original allocation size was non-zero), and couldn't grow in place, so we have to copy the old data
    // as this is only for useful bytes, we use the sizes that are given out to the application here, not the allocation sizes
    wasm_memory_copy(newAllocation.address.toInt(), originalPtr, minOf(originalSize, newSize))
    // also free the old allocation (from which we copied), which is now useless
    FreeList.free(MemorySlot(Pointer(originalPtr.toUInt()), originalAllocationSize))

    return newAllocation.address.toInt()
}

/**
 *  Frees memory allocated by all previous calls of [componentModelRealloc].
 *
 *  NOTE: This function is incompatible with freeing memory manually through `componentModelRealloc(ptr, size, 0)` calls, as this will result in a double-free.
 *  TODO(REVIEW): Try to automatically handle these cases? Would make everything a bit uglier, but also reduce the chances of people running into double-frees.
 */
@OptIn(UnsafeWasmMemoryApi::class)
@Deprecated("Freeing all cabi_realloc-allocated memory is incompatible with the WASI preview 1 to preview 2 adapter which uses cabi_realloc allocated memory in a persistent way (never frees it, expects it to essentially have static storage duration). This means that the use of this function will always cause use-after-free UB in the adapter.")
@ComponentModelInternalApi
public fun freeAllComponentModelReallocAllocatedMemory() {
    // TODO need to reenable this before merging
//    if (reallocAllocator != null) {
//        reallocAllocator!!.destroy()
//        reallocAllocator = null
//    }
}

// TODO(REVIEW) is this okay? is only for testing purposes
@OptIn(UnsafeWasmMemoryApi::class)
internal fun dumpFreeList(): String {
    return FreeList.debugDump()
}
