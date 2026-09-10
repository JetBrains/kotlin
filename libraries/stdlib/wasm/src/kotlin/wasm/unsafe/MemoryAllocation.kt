/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.wasm.unsafe

import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import kotlin.internal.DoNotInlineOnFirstStage
import kotlin.text.clear
import kotlin.text.iterator
import kotlin.wasm.ExperimentalWasmInterop
import kotlin.wasm.internal.wasm_memory_copy
import kotlin.wasm.internal.wasm_memory_grow
import kotlin.wasm.internal.wasm_memory_size
import kotlin.wasm.internal.wasm_unreachable
import kotlin.wasm.unsafe.FreeListAllocator.FreeList.list

/**
 * WebAssembly linear memory allocator.
 */
@UnsafeWasmMemoryApi
@ExperimentalWasmInterop
public abstract class MemoryAllocator {
    /**
     * Allocates a block of uninitialized linear memory of the given [size] in bytes.
     *
     * [size] must be >= 0. Zero-size allocations are allowed, but the resulting pointer may not be dereferenced.
     *
     * @return an address pointing to [size] allocated bytes. It is guaranteed to be a multiple of 8. It is not meaningful to compare these addresses or perform pointer arithmetic.
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
@ExperimentalWasmInterop
@DoNotInlineOnFirstStage
public inline fun <T> withScopedMemoryAllocator(
    block: (allocator: MemoryAllocator) -> T,
): T {
    contract { callsInPlace(block, InvocationKind.EXACTLY_ONCE) }
    val allocator = createAllocatorInTheNewScope()
    val result = try {
        block(allocator)
    } finally {
        allocator.destroy()
    }
    return result
}

@PublishedApi
@UnsafeWasmMemoryApi
@ExperimentalWasmInterop
internal fun createAllocatorInTheNewScope(): ScopedMemoryAllocator {
    val allocator = ScopedMemoryAllocator(0, parent = null)
    return allocator
}

@UnsafeWasmMemoryApi
@ExperimentalWasmInterop
private data class MemorySlot(val ptr: Pointer, val size: UInt) {
    companion object {
        /**
         * Returns true if the two slots can be merged into one slot, by being exactly adjacent. As allocated memory slots cannot overlap, overlap isn't allowed for the purposes of merging either.
         */
        fun canMerge(left: MemorySlot, right: MemorySlot): Boolean {
            assert(left.ptr.address != right.ptr.address) { "Allocated slots cannot describe memory starting at the exact same address" }

            assert(left.ptr.address + left.size <= right.ptr.address) { "Left slot be left of the right slot, and be adjacent at max, may not be overlap" }

            return left.ptr.address + left.size == right.ptr.address
        }
    }

    fun tryMerge(other: MemorySlot): MemorySlot? {
        val [left, right] = if (ptr.address < other.ptr.address)
            this to other
        else
            other to this

        if (canMerge(left, right)) { // left extends to just next to the right
            // the combined allocation end is one of the previous ends, just whichever is larger
            val newEndAddr = maxOf(left.ptr.address + left.size, right.ptr.address + right.size)
            val newSize = newEndAddr - left.ptr.address
            return MemorySlot(left.ptr, newSize)
        }

        return null
    }
}

private val alignment = 8u

/**
 * Returns size but possibly lengthened to align to an implementation-defined alignment.
 *
 * This means it represents the actual size of any allocation made with the size parameter.
 *
 * The alignment is currently 8, as it's currently the maximum needed for the Wasm component model canonical ABI.
 */
private fun realAllocationSize(size: UInt): UInt {
    // TODO(REVIEW): if this function is passed 0xfff..fff, it will overflow to 0. Semantically this sort of makes sense, and we don't allow allocations of that size anyway. We could check() this here, I don't feel strongly about this one way or another.

    // round up the size to a multiple of 8, so that all addresses are always guaranteed to be aligned to at least 8
    // by adding 7, we're guaranteed to:
    // - if size mod 8 == 0: NOT cross the divisible-by-8 boundary
    // - if size mod 8 != 0: cross the divisible-by-8 boundary exactly once.
    // so after adding 7, just shave off the lower bits under 7
    return (size + (alignment - 1u)) and (alignment - 1u).inv()
    //     (size + 7u              )  &   0xFFFFFFF8u
}

/// Reserve 0 and don't give it out as a valid address
private val firstValidAddress = alignment


@UnsafeWasmMemoryApi
@ExperimentalWasmInterop
private object FreeListAllocator {
    // TODO(REVIEW): See end of this file
    fun debugDump(): String = buildString {
        appendLine("FreeList:")
        for (it in list) {
            appendLine("  ${it.ptr.address} - ${it.ptr.address + it.size}")
        }
    }

    // NOTES:
    // - design choice for now: store all the info here (i.e., in WasmGC structs), instead of trying to be clever and use headers in linear memory or similar.
    //   - no obvious advantage to using headers
    // - design distinction between FreeList and FreeListAllocator:
    //   - FreeList itself only deals with an abstract, mathematical notion of memory slots, not with actual allocations that need to be requested from the environment, nor with API-specific details like zero-size allocations, or how errors are exposed to the user
    // - NOT thread-safe, would need synchronization if not used in a single-threaded environment
    // - NOT reentrant, i.e., cannot call any member functions of this, from within any member functions of this
    @UnsafeWasmMemoryApi
    @ExperimentalWasmInterop
    private object FreeList {
        // NOTE: this uses an array list. That's not really optimal, because it requires copying around stuff when the number of free slots change
        val list = mutableListOf<MemorySlot>(
            MemorySlot(Pointer(firstValidAddress), ((1u shl 31) - 1u))
        )

        /**
         * TODO(REVIEW) can we get rid of this / make it test only? Should we keep it even in production?
         *
         * Make sure to not call call free / alloc from anywhere inside free / alloc
         */
        private var isAlreadyOperating = false

        /**
         * Allocates a slot in the free list.
         */
        fun allocate(size: UInt): MemorySlot? {
            check(!isAlreadyOperating) { "Cannot call allocate from within the allocator" }
            isAlreadyOperating = true
            try {
                check(size > 0u) { "Cannot allocate zero-size slot" }

                val alignedSize = realAllocationSize(size)

                val slotIndex = list.indexOfFirst { it.size >= alignedSize }
                // no more memory to give out
                if (slotIndex == -1)
                    return null

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

        // NOTE: freeing is when things are merged back together

        /**
         * Frees an allocated slot. May only be passed memory slots that originate from [allocate].
         */
        fun free(allocatedSlot: MemorySlot) {
            check(!isAlreadyOperating) { "Cannot call free from within the allocator" }
            isAlreadyOperating = true
            try {
                check(allocatedSlot.size > 0u) { "Slot to free clearly does not originate from free-list allocation: allocated slot size is zero" }
                check(allocatedSlot.size % alignment == 0u) { "Slot to free clearly does not originate from free-list allocation: allocated slot size is not a multiple of alignment" }

                // need to find the slots that this lies in between, in terms of start address
                // NOTE: we assume (and later assert) the allocatedSlot does not overlap with anything in the free list, that wouldn't make sense, by definition, allocatedSlot is not free
                val minusInsertionPointMinusOne = list.binarySearch {
                    // because we assume it can't overlap, we know that size is irrelevant here: we'll get the index of the insertion point from this function, and inserting there will not lead to overlap
                    (it.ptr.address.toLong() - allocatedSlot.ptr.address.toLong()).toInt()
                }

                require(minusInsertionPointMinusOne != 0) { "Double-free: slot to free can't already be in the free list; slot to free: $allocatedSlot; " + debugDump() }

                // convert back to actual insertion point
                val insertionPointIndex = -(minusInsertionPointMinusOne + 1)

                // before we insert, try to merge
                val leftElement = list.getOrNull(insertionPointIndex - 1)
                val rightElement = list.getOrNull(insertionPointIndex)
                assert(
                    (leftElement == null || (leftElement.ptr.address + leftElement.size <= allocatedSlot.ptr.address)) &&
                            (rightElement == null || (allocatedSlot.ptr.address + allocatedSlot.size <= rightElement.ptr.address))
                ) { "Slot to free overlaps with an existing slot" }

                // 4 basic cases ("<" meaning "cannot merge", ">=" meaning "can merge"):
                // 1. (end of left) < (start of new)  && (end of new) <  (start of right)
                //    Can't merge anything -> insert only
                // 2. (end of left) < (start of new)  && (end of new) >= (start of right)
                //    Can only merge one slot, so replace that one
                // 3. (end of left) >= (start of new) && (end of new) <  (start of right)
                //    Symmetrical to 2.
                // 4. (end of left) >= (start of new) && (end of new) >= (start of right)
                //    Can merge everything into one, so replace one, remove one

                val canMergeLeft = leftElement != null && MemorySlot.canMerge(leftElement, allocatedSlot)
                val canMergeRight = rightElement != null && MemorySlot.canMerge(allocatedSlot, rightElement)
                when {
                    !canMergeLeft && !canMergeRight -> {
                        list.add(insertionPointIndex, allocatedSlot)
                    }
                    canMergeLeft && !canMergeRight -> {
                        list[insertionPointIndex - 1] = leftElement.tryMerge(allocatedSlot)!!
                    }
                    !canMergeLeft && canMergeRight -> {
                        list[insertionPointIndex] = allocatedSlot.tryMerge(rightElement)!!
                    }
                    canMergeLeft && canMergeRight -> {
                        list[insertionPointIndex - 1] = leftElement.tryMerge(allocatedSlot)!!.tryMerge(rightElement)!!
                        list.removeAt(insertionPointIndex)
                    }
                }
            } finally {
                isAlreadyOperating = false
            }
        }
    }

    fun allocate(size: Int): MemorySlot {
        check(size >= 0) { "Cannot allocate negative size" }

        // the free list does not know about zero-size allocations
        if (size == 0)
            return MemorySlot(Pointer(firstValidAddress), 0u)

        val result = FreeList.allocate(size.toUInt())
            ?: throw OutOfMemoryError("Out of linear memory. All available address space (2gb) is used.")

        assert(result.ptr.address % 8u == 0u) { "Allocation result must be at least 8-byte aligned" }

        val firstInvalidAddress = wasm_memory_size().toUInt() * WASM_PAGE_SIZE_IN_BYTES.toUInt()
        val endAddressExclusive = result.ptr.address.toULong() + result.size
        if (endAddressExclusive >= firstInvalidAddress) {
            val numPagesToGrow =
                (endAddressExclusive - firstInvalidAddress) / WASM_PAGE_SIZE_IN_BYTES.toUInt() + 2u

            if (wasm_memory_grow(numPagesToGrow.toInt()) == -1) {
                throw OutOfMemoryError("Out of linear memory. memory.grow returned -1")
            }
        }

        check(endAddressExclusive < wasm_memory_size().toUInt() * WASM_PAGE_SIZE_IN_BYTES.toUInt())

        return result
    }

    fun free(allocatedSlot: MemorySlot) {
        // zero-size slots are handed out, so freeing them is a no-op, as they were never taken from the free list
        if (allocatedSlot.size == 0u)
            return

        FreeList.free(allocatedSlot)
    }

}

@ExperimentalWasmInterop
@UnsafeWasmMemoryApi
private class ArenaLikeAllocator : MemoryAllocator() {
    private val allocationsToFree = mutableListOf<MemorySlot>()

    override fun allocate(size: Int): Pointer {
        val allocation = FreeListAllocator.allocate(size)

        // track this allocation so they can all be freed on destroy()
        allocationsToFree.add(allocation)

        return allocation.ptr
    }

    // NOTE: we don't expose a free() function directly, to a) make it harder to write use-after-frees/double-frees, and b) not expose MemorySlot / FreeList beyond this file

    @PublishedApi
    internal fun destroy() {
        // NOTE: this could be optimized by first finding the indices of what to merge and remove, and then shrinking the list all at once, to minimize the amount of copying that's necessary
        for (allocation in allocationsToFree) {
            FreeListAllocator.free(allocation)
        }
        allocationsToFree.clear()
    }
}

// TODO(KT-89320): Consider removing this class and fully replacing it with ArenaLikeAllocator
// TODO(KT-58041): Consider switching back to using ULong
@PublishedApi
@UnsafeWasmMemoryApi
@ExperimentalWasmInterop
internal class ScopedMemoryAllocator(
    // TODO(REVIEW): Probably can't remove these because they're part of the @PublishedApi right? They don't have a use anymore.
    startAddress: Int,
    @PublishedApi
    internal var parent: ScopedMemoryAllocator?,
) : MemoryAllocator() {

    private val delegatingAllocator = ArenaLikeAllocator()

    @Suppress("UNUSED")
    @PublishedApi
    internal fun createChild(): ScopedMemoryAllocator {
        return createAllocatorInTheNewScope()
    }

    override fun allocate(size: Int): Pointer = delegatingAllocator.allocate(size)

    @PublishedApi
    internal fun destroy() = delegatingAllocator.destroy()
}

private const val WASM_PAGE_SIZE_IN_BYTES = 65_536  // 64 KiB

@OptIn(UnsafeWasmMemoryApi::class, ExperimentalWasmInterop::class)
private var reallocAllocator: ScopedMemoryAllocator? = null

/**
 * WebAssembly Component Model Canonical ABI realloc implementation.
 * This function is intended to be exported for Component Model support and must not be called directly!
 *
 * Memory allocated by this function must be freed by either:
 * - calling this function again with the original pointer, the original size, and new size 0.
 * - calling [freeAllComponentModelReallocAllocatedMemory] (without freeing *any* memory manually)
 */
@OptIn(UnsafeWasmMemoryApi::class, ExperimentalWasmInterop::class)
@ComponentModelInternalApi
public fun componentModelRealloc(
    originalPtr: Int,
    originalSize: Int,
    newSize: Int,
): Int {
    check(newSize >= 0) { "Cannot allocate negative size" }

    // The first call to realloc creates a new allocator.
    // TODO(KT-89312): we use a separate allocator here, only to be able to `freeAllComponentModelReallocAllocatedMemory`. Once this function is removed, we can simplify the implementation of component model realloc to simply use the FreeListAllocator only.
    if (reallocAllocator == null) {
        reallocAllocator = createAllocatorInTheNewScope()
    }
    val allocator = reallocAllocator!!

    try {
        // to address the correct slot, we must extend the original size to be aligned, as that will be the internal size of the slot
        val originalAllocationSize = realAllocationSize(originalSize.toUInt())

        if (newSize == 0) {
            if (originalPtr == 0 && originalSize == 0) // this is a zero-size allocation request, not a free (freeing 0 and allocating 0 are both no-ops, only need to ensure the correct return value)
                return allocator.allocate(0).address.toInt()

            // TODO(REVIEW) this is an easy way to get the program to throw, if it's misused. Any possible guardrails against this?
            FreeListAllocator.free(MemorySlot(Pointer(originalPtr.toUInt()), originalAllocationSize))
            return -1 // TODO(REVIEW) better return value? -1 most clearly indicates "not a valid address"
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
            FreeListAllocator.free(MemorySlot(Pointer(originalPtr.toUInt() + newAllocationSize), originalAllocationSize - newAllocationSize))
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
            FreeListAllocator.free(MemorySlot(Pointer(startOfOverallocatedMemory), overallocatedSize))
            // TODO(REVIEW) comment too long?
            // NOTE: allocating and then freeing again might seem overcomplicated; the obvious alternative would be to allocate twice in a row instead. The reason not to allocate twice is as follows:
            //       if the allocator ever changes, and stops giving out contiguous memory, this code path (overallocating, then freeing) will simply stop being used, and nothing will break.
            //       While the allocation does occur contiguously, the free is also contiguous and doesn't perform any complex logic, because all that changes is the start address of the free list block that we're allocating from, the list itself is not modified.
            //
            //       If we instead allocated twice, in case we can't grow the original allocation in place, we're implicitly relying on being able to perform the second allocation in place. This isn't always true, and would create further complications in these cases, by having to free the "failed" allocation first. Thus, overallocating plus freeing is safer than allocating incrementally.
            //       Conversely, this implementation suffers from sometimes not being able to grow an allocation in place, when the initial overallocatedSize is too large, even though the real needed size would be small enough to fit. However, this only results in an additional copy, instead of a semantics change.

            return originalPtr
        }

        // case 3c: we now know the allocation grew (and the original allocation size was non-zero), and couldn't grow in place, so we have to copy the old data
        // as this is only for useful bytes, we use the sizes that are given out to the application here, not the allocation sizes
        wasm_memory_copy(newAllocation.address.toInt(), originalPtr, minOf(originalSize, newSize))
        // also free the old allocation (from which we copied), which is now useless
        FreeListAllocator.free(MemorySlot(Pointer(originalPtr.toUInt()), originalAllocationSize))

        return newAllocation.address.toInt()
    } catch (e: OutOfMemoryError) {
        // the canonical ABI specifies realloc must trap via unreachable in this case
        wasm_unreachable()
    }
}

// TODO(KT-89312): Remove this function, and simplify componentModelRealloc
/**
 *  Frees memory allocated by all previous calls of [componentModelRealloc]. This is intended to be used for Component Model support and must not be called directly!
 *
 *  NOTE: This function is incompatible with freeing memory manually through `componentModelRealloc(ptr, size, 0)` calls, as this will result in a double-free.
 *  TODO(REVIEW): Try to automatically handle these cases? Would make everything a bit uglier, but also reduce the chances of people running into double-frees (though that would only throw, not corrupt internal state).
 */
@OptIn(UnsafeWasmMemoryApi::class, ExperimentalWasmInterop::class)
@Deprecated("Freeing all cabi_realloc-allocated memory is incompatible with the WASI preview 1 to preview 2 adapter which uses cabi_realloc to allocate memory that it expects is never freed. This means that the use of this function will always cause use-after-free UB in the adapter.")
@ComponentModelInternalApi
public fun freeAllComponentModelReallocAllocatedMemory() {
    if (reallocAllocator != null) {
        reallocAllocator!!.destroy()
        reallocAllocator = null
    }
}

// TODO(REVIEW) do we have a better solution for this? only exists for testing purposes
@OptIn(UnsafeWasmMemoryApi::class, ExperimentalWasmInterop::class)
internal fun dumpFreeList(): String {
    return FreeListAllocator.debugDump()
}
