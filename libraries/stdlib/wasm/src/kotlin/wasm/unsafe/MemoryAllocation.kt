/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.wasm.unsafe

import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import kotlin.internal.DoNotInlineOnFirstStage
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
     * [size] must be >= 0. Zero-size allocations are allowed, and the resulting pointer will be 8-byte aligned, but may not be dereferenced.
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
    init {
        assert(ptr.address.toULong() + size.toULong() <= globalFirstInvalidAddress) { "Memory slot must not exceed the maximum address" }
    }

    companion object {
        /**
         * Returns true if the two slots can be merged into one slot, by being exactly adjacent. As allocated memory slots cannot overlap, overlap isn't allowed for the purposes of merging either.
         */
        fun canMerge(left: MemorySlot, right: MemorySlot): Boolean {
            assert(left.ptr.address != right.ptr.address) { "Allocated slots cannot describe memory starting at the exact same address" }

            assert(left.ptr.address + left.size <= right.ptr.address) { "Left slot be left of the right slot, and be adjacent at max, may not be overlap" }

            return left.ptr.address + left.size == right.ptr.address
        }

        /**
         * Merges [left] with [right]; assumes that the end of [left] lines up exactly with the start of [right], i.e., they [canMerge]
         */
        fun merge(left: MemorySlot, right: MemorySlot): MemorySlot {
            assert(canMerge(left, right)) { "End of left slot must line up perfectly with start of right slot" }

            return MemorySlot(left.ptr, left.size + right.size)
        }
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
    // round up the size to a multiple of 8, so that all addresses are always guaranteed to be aligned to at least 8
    // by adding 7, we're guaranteed to:
    // - if size mod 8 == 0: NOT cross the divisible-by-8 boundary
    // - if size mod 8 != 0: cross the divisible-by-8 boundary exactly once.
    // so after adding 7, just shave off the lower bits under 7
    val alignedSize =  (size + (alignment - 1u)) and (alignment - 1u).inv()
    //                 (size + 7u              )  &   0xFFFFFFF8u

    // 0xfff..fff overflows to 0. This is an invalid sue of this function (which is file-private), so we only check it in tests (when assertions are turned on), not in production code
    assert(size <= alignedSize) { "Aligned size must be at least as large as the original size" }

    return alignedSize
}

/// Reserve 0 and don't give it out as a valid address
private val globalFirstValidAddress = alignment

// we only give out positive Int addresses, i.e. only around max 2GiB
private val globalFirstInvalidAddress = Int.MAX_VALUE.toUInt()


@UnsafeWasmMemoryApi
@ExperimentalWasmInterop
private object FreeListAllocator {
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
            MemorySlot(Pointer(globalFirstValidAddress), globalFirstInvalidAddress - globalFirstValidAddress)
        )

        /**
         * Make sure to not call call free / alloc from anywhere inside free / alloc. This is dead code in production, as it's only read by `assert`s, which are compiled out in production.
         */
        private var isAlreadyOperating = false

        /**
         * Allocates a slot in the free list.
         */
        fun allocate(size: UInt): MemorySlot? {
            assert(!isAlreadyOperating) { "Cannot call allocate from within the allocator" }
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
            assert(!isAlreadyOperating) { "Cannot call free from within the allocator" }
            isAlreadyOperating = true
            try {
                check(allocatedSlot.size > 0u) { "Slot to free clearly does not originate from free-list allocation: allocated slot size is zero" }
                check(allocatedSlot.size % alignment == 0u) { "Slot to free clearly does not originate from free-list allocation: allocated slot size is not a multiple of alignment" }

                val insertionPointIndex = findInsertionPoint(allocatedSlot)

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
                        list[insertionPointIndex - 1] = MemorySlot.merge(leftElement, allocatedSlot)
                    }
                    !canMergeLeft && canMergeRight -> {
                        list[insertionPointIndex] = MemorySlot.merge(allocatedSlot, rightElement)
                    }
                    canMergeLeft && canMergeRight -> {
                        list[insertionPointIndex - 1] = MemorySlot.merge(MemorySlot.merge(leftElement, allocatedSlot), rightElement)
                        list.removeAt(insertionPointIndex)
                    }
                }
            } finally {
                isAlreadyOperating = false
            }
        }

        /**
         * Finds the free slots that this allocated slot lies in between, and returns the insertion point, i.e., the index of the right of those two slots. Thus if this index is used to insert the allocated slot, the list will remain sorted.
         *
         * This function `require()`s that the [allocatedSlot] does not overlap with anything in the free list (i.e., that it follows the definition of an allocated slot).
         */
        private fun findInsertionPoint(allocatedSlot: MemorySlot): Int {
            // NOTE: we assume (and later assert) the allocatedSlot does not overlap with anything in the free list, that wouldn't make sense, by definition, allocatedSlot is not free
            val minusInsertionPointMinusOne = list.binarySearch {
                // because we assume it can't overlap, we know that size is irrelevant here: we'll get the index of the insertion point from this function, and inserting there will not lead to overlap
                (it.ptr.address.toLong() - allocatedSlot.ptr.address.toLong()).toInt()
            }

            require(minusInsertionPointMinusOne != 0) { "Double-free or use-after-free: allocated slot can't already be in the free list; allocated slot: $allocatedSlot; " + debugDump() }

            // convert back to actual insertion point
            val insertionPointIndex = -(minusInsertionPointMinusOne + 1)
            return insertionPointIndex
        }

        fun tryGrowInPlace(allocatedSlot: MemorySlot, newSize: UInt): MemorySlot? {
            assert(!isAlreadyOperating) { "Cannot call tryGrowInPlace from within the allocator" }
            isAlreadyOperating = true
            try {
                assert(newSize % alignment == 0u) { "Attempted to grow an allocation to a size that is not aligned" }

                val additionalSize = newSize - allocatedSlot.size

                val indexOfRight = findInsertionPoint(allocatedSlot)
                if (indexOfRight == list.size) {
                    // super-edge case of trying to grow an allocation to a size that extends to the end of the entire address space. Not possible to grow in place.
                    return null
                }

                val rightSlot = list[indexOfRight]

                // The sufficient condition to grow in place, is that the right-side slot (which is maximally large, as we merge freed slots together eagerly) is 1. contiguous with the allocatedSlot, and 2. is large enough to accommodate the additional size. If that's not the case, we cannot grow in place.
                if (!(rightSlot.ptr.address == allocatedSlot.ptr.address + allocatedSlot.size &&
                            rightSlot.size >= additionalSize)) {
                    return null
                }

                // To grow the existing slot, shrink the right slot: shift it by the additional size, and reduce its size by the additional size
                val newRightSlot = MemorySlot(ptr = Pointer(rightSlot.ptr.address + additionalSize), rightSlot.size - additionalSize)
                if (newRightSlot.size == 0u) {
                    list.removeAt(indexOfRight)
                } else {
                    list[indexOfRight] = newRightSlot
                }

                // we return the new version of the allocated slot: it starts at the same point, and has the extended size
                return allocatedSlot.copy(size = newSize)
            } finally {
                isAlreadyOperating = false
            }
        }
    }

    fun allocate(size: Int): MemorySlot {
        check(size >= 0) { "Cannot allocate negative size" }

        // the free list does not know about zero-size allocations
        if (size == 0)
            // even when allocating size 0, we need to grow to ensure the address is <= wasm_memory_size(), otherwise the VM might trap
            return MemorySlot(Pointer(globalFirstValidAddress), 0u).growRealMemoryIfNeeded()

        val result = FreeList.allocate(size.toUInt())
            ?: throw OutOfMemoryError("Out of linear memory. All available address space (2gb) is used.")

        assert(result.ptr.address % 8u == 0u) { "Allocation result must be at least 8-byte aligned" }

        return result.growRealMemoryIfNeeded()
    }

    /**
     * Grows the actual wasm linear memory to accomodate this memory slot. Returns itself, so that
     * `return MemorySlot(...).growRealMemoryIfNeeded()` works, which reads nicely.
     */
    private fun MemorySlot.growRealMemoryIfNeeded(): MemorySlot {
        val newAllocation = this

        val firstInvalidAddress = wasm_memory_size().toUInt() * WASM_PAGE_SIZE_IN_BYTES.toUInt()
        val endAddressExclusive = newAllocation.ptr.address.toULong() + newAllocation.size
        if (endAddressExclusive >= firstInvalidAddress) {
            val numPagesToGrow =
                (endAddressExclusive - firstInvalidAddress) / WASM_PAGE_SIZE_IN_BYTES.toUInt() + 2u

            if (wasm_memory_grow(numPagesToGrow.toInt()) == -1) {
                throw OutOfMemoryError("Out of linear memory. memory.grow returned -1")
            }
        }

        check(endAddressExclusive < wasm_memory_size().toUInt() * WASM_PAGE_SIZE_IN_BYTES.toUInt())
        return newAllocation
    }

    fun free(allocatedSlot: MemorySlot) {
        // zero-size slots are handed out, so freeing them is a no-op, as they were never taken from the free list
        if (allocatedSlot.size == 0u)
            return

        FreeList.free(allocatedSlot)
    }

    fun tryGrowInPlace(allocatedSlot: MemorySlot, newSize: UInt): MemorySlot? {
        return FreeList.tryGrowInPlace(allocatedSlot, newSize)?.growRealMemoryIfNeeded()
    }

}

@ExperimentalWasmInterop
@UnsafeWasmMemoryApi
private class ArenaLikeAllocator : MemoryAllocator() {
    private val allocationsToFree = mutableListOf<MemorySlot>()

    internal fun dumpAllocationsToFree(): String {
        return allocationsToFree.joinToString("\n") { it.toString() }
    }

    override fun allocate(size: Int): Pointer {
        val allocation = FreeListAllocator.allocate(size)

        if (size == 0) // do not track zero-size allocations, they don't need to be freed
            return allocation.ptr

        // track this allocation so they can all be freed on destroy()
        allocationsToFree.add(allocation)

        return allocation.ptr
    }

    private fun trackedIndexOf(ptr: Pointer): Int {
        val i = allocationsToFree.indexOfFirst { it.ptr == ptr }
        require(i != -1) { "Attempted to free/modify an allocation that was not allocated by this allocator" }
        return i
    }

    fun free(ptr: Pointer, originalSize: UInt) {
        if (originalSize == 0u) // freeing zero-size allocations is a noop, and they aren't tracked
            return

        val singleAllocation = allocationsToFree[trackedIndexOf(ptr)]

        require(originalSize == singleAllocation.size) { "Attempted to free an allocation with a different size than expected" }

        val allocationExisted = allocationsToFree.remove(singleAllocation)
        assert(allocationExisted) { "Attempted to free an allocation that was not allocated by this allocator" }
        FreeListAllocator.free(singleAllocation)
    }

    fun shrinkInPlace(ptr: Pointer, newSize: UInt) {
        assert(newSize % alignment == 0u) { "Attempted to shrink an allocation to a size that is not aligned" }

        // NOTE: because we're only subtracting aligned sizes, the result will still be aligned

        val singleAllocationIndex = trackedIndexOf(ptr)
        val singleAllocation = allocationsToFree[singleAllocationIndex]


        val originalSize = singleAllocation.size
        require(newSize <= originalSize) { "Attempted to shrink an allocation to a larger size" }

        // internally, what needs to be freed is the chunk starting at the point that we don't need anymore, i.e., the allocation start plus the new size. That part is of size (originalSize - newSize)
        FreeListAllocator.free(MemorySlot(Pointer(singleAllocation.ptr.address + newSize), originalSize - newSize))

        // keep track of the new allocation size
        allocationsToFree[singleAllocationIndex] = singleAllocation.copy(size = newSize)
    }

    fun tryGrowInPlace(ptr: Pointer, newSize: UInt): Boolean {
        val singleAllocationIndex = trackedIndexOf(ptr)
        val singleAllocation = allocationsToFree[singleAllocationIndex]

        val newSingleAllocation = FreeListAllocator.tryGrowInPlace(singleAllocation, newSize)
            // if we don't get a new allocation, the grow-in-place request has failed, so we simply stop
            ?: return false

        allocationsToFree[singleAllocationIndex] = newSingleAllocation

        return true
    }

    fun destroy() {
        // NOTE: this could be optimized by first finding the indices of what to merge and remove, and then shrinking the list all at once, to minimize the amount of copying that's necessary
        for (allocation in allocationsToFree) {
            // NOTE: we don't use the class-internal free here, as it only slows us down by editing the list in between, which isn't necessary, as we clear the entire list here
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
private var reallocAllocator: ArenaLikeAllocator? = null

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
        reallocAllocator = ArenaLikeAllocator()
    }
    val allocator = reallocAllocator!!

    try {
        // to address the correct slot, we must extend the original size to be aligned, as that will be the internal size of the slot
        val originalAllocationSize = realAllocationSize(originalSize.toUInt())

        if (newSize == 0) {
            if (originalPtr == 0 && originalSize == 0) // this is a zero-size allocation request, not a free (freeing 0 and allocating 0 are both no-ops, only need to ensure the correct return value)
                return allocator.allocate(0).address.toInt()

            allocator.free(Pointer(originalPtr.toUInt()), originalAllocationSize)
            return -1
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
            allocator.shrinkInPlace(Pointer(originalPtr.toUInt()), newAllocationSize)
            return originalPtr
        }

        // case 3: growing, i.e., we need to do some actual allocation

        // case 3a: the original size was 0, hand out a new allocation of the new size
        if (originalSize == 0) {
            val newAllocation = allocator.allocate(newSize)
            return newAllocation.address.toInt()
        }

        // We know this from the cases before, assert it only to be defensive in case of changes to this function
        assert(originalAllocationSize <= newAllocationSize) { "Shrinking & non-growing cases should already be handled" }

        // try to grow the allocation in place
        if (allocator.tryGrowInPlace(Pointer(originalPtr.toUInt()), newAllocationSize)) {
            // case 3b: we can grow the allocation in place:
            // - The `try` call above was successful -> the necessary freeing and state management are done.
            // - There's no copying necessary, as the allocation with the old data is still in place.

            return originalPtr
        }

        // case 3c: we now know the allocation grew (and the original allocation size was non-zero), and couldn't grow in place, so we have to perform an actual new allocation, and copy the old data
        val newAllocation = allocator.allocate(newSize)
        // as this is only for useful bytes, we use the sizes that are given out to the application here, not the allocation sizes
        wasm_memory_copy(newAllocation.address.toInt(), originalPtr, minOf(originalSize, newSize))
        // also free the old allocation (from which we copied), which is now useless
        allocator.free(Pointer(originalPtr.toUInt()), originalAllocationSize)

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

@OptIn(UnsafeWasmMemoryApi::class, ExperimentalWasmInterop::class)
internal fun dumpFreeList(): String {
    return FreeListAllocator.debugDump()
}

@OptIn(UnsafeWasmMemoryApi::class, ExperimentalWasmInterop::class)
internal fun dumpReallocAllocatorTrackedAllocations(): String? {
    return reallocAllocator?.dumpAllocationsToFree()
}
