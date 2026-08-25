package test.wasm.unsafe

import kotlin.wasm.unsafe.*
import kotlin.test.*

@OptIn(UnsafeWasmMemoryApi::class, ComponentModelInternalApi::class)
class ReallocTest {
    var freelist: String = ""

    // make sure that the state of the freelist is exactly the same before and after each test, i.e. all allocated memory is freed
    @BeforeTest
    fun saveFreelist() {
        freelist = dumpFreeList()
    }

    @AfterTest
    fun compareFreelist() {
        assertEquals(freelist, dumpFreeList())
    }

    @Test
    fun reallocFreeAllTest(){
        componentModelRealloc(0, 0, 100)
        componentModelRealloc(0, 0, 200)
        componentModelRealloc(0, 0, 1)
        // make sure that all memory is actually freed by this function
        @Suppress("DEPRECATION")
        freeAllComponentModelReallocAllocatedMemory()
    }

    @Test
    fun freshReallocTest() {
        val address1 = componentModelRealloc(0, 0, 10)
        val address2 = componentModelRealloc(0, 0, 10)
        assertNotEquals(address1, address2)

        // free it, FIFO order
        componentModelRealloc(address1, 10, 0)
        componentModelRealloc(address2, 10, 0)

        val address3 = componentModelRealloc(0, 0, 10)
        val address4 = componentModelRealloc(0, 0, 10)

        // After freeing memory, new reallocs should reuse the old memory
        assertEquals(address1, address3)
        assertEquals(address2, address4)

        // now free it in LIFO order
        componentModelRealloc(address4, 10, 0)
        componentModelRealloc(address3, 10, 0)

        // And again check that new allocations reuse the freed memory
        val address5 = componentModelRealloc(0, 0, 10)
        val address6 = componentModelRealloc(0, 0, 10)

        assertEquals(address1, address5)
        assertEquals(address2, address6)

        componentModelRealloc(address5, 10, 0)
        componentModelRealloc(address6, 10, 0)
    }

    @Test
    fun reallocInPlaceTest() {
        val allocationStepSize = 10
        val numReallocs = 20
        val address1 = componentModelRealloc(0, 0, allocationStepSize)
        repeat(numReallocs - 1) { i ->
            val newAddress1 = componentModelRealloc(
                originalPtr = address1,
                originalSize = (i + 1) * allocationStepSize,
                newSize = (i + 2) * allocationStepSize
            )
            assertEquals(address1, newAddress1)
        }

        val address2 = componentModelRealloc(0, 0, 10)
        assertTrue(address2 - address1 >= allocationStepSize * numReallocs)

        componentModelRealloc(address1, numReallocs * allocationStepSize, 0)
        componentModelRealloc(address2, 10, 0)
    }

    private fun writeNBytes(address: Int, n: Int, value: Byte) {
        repeat(n) { i ->
            Pointer((address + i).toUInt()).storeByte(value)
        }
    }

    private fun assertBytesEquals(address: Int, n: Int, expected: Byte) {
        repeat(n) { i ->
            assertEquals(expected, Pointer((address + i).toUInt()).loadByte())
        }
    }

    @Test
    fun reallocWithCopyTest() {
        val bufferSize = 100

        // Zero-out large chunk of memory for the following tests
        val sizeToClean = bufferSize * 100
        val addrToClean = componentModelRealloc(0, 0, sizeToClean)
        writeNBytes(addrToClean, sizeToClean, 0.toByte())
        assertBytesEquals(addrToClean, sizeToClean, 0.toByte())
        // free
        componentModelRealloc(addrToClean, sizeToClean, 0)

        val address1 = componentModelRealloc(0, 0, bufferSize)
        writeNBytes(address1, bufferSize, 1.toByte())

        val address2 = componentModelRealloc(0, 0, bufferSize)
        writeNBytes(address2, bufferSize, 2.toByte())

        // Realloc address1 "in the middle" of the bump allocator stack, forcing it to be reallocated and data to be copied
        val address1new = componentModelRealloc(address1, bufferSize, bufferSize * 2)
        assertNotEquals(address1, address1new)
        assertTrue(address1new > address2)
        assertBytesEquals(address1new, bufferSize, 1.toByte())

        // Now address2 is also not at the top of the allocation stack. It will be copied too
        val address2new = componentModelRealloc(address2, bufferSize, bufferSize * 2)
        assertNotEquals(address2, address2new)
        assertTrue(address2new > address1new)
        assertBytesEquals(address2new, bufferSize, 2.toByte())

        // free
        componentModelRealloc(address1new, bufferSize * 2, 0)
        componentModelRealloc(address2new, bufferSize * 2, 0)
    }

    @Test
    fun reallocInNestedScopeTest() {
        withScopedMemoryAllocator { allocator ->
            val scopedAddr = allocator.allocate(10)
            val reallocAddr = componentModelRealloc(0, 0, 10)
            assertTrue(reallocAddr.toUInt() > scopedAddr.address)
            assertTrue((reallocAddr.toUInt() - scopedAddr.address) >= 10u)

            componentModelRealloc(reallocAddr, 10, 0)

            val scopedAddr2 = allocator.allocate(10)
            assertEquals(scopedAddr2.address.toInt(), reallocAddr)
        }
    }


    @Test
    fun reallocShrinkingDoesPartialFreeCorrectlyTest() {
        // allocated a big chunk, then shrunk the allocation: should free the excess memory, and thus place the next allocation in there

        // so to get a benchmark, first allocate some memory normally, and do another allocation, this one will have to be reproduced exactly by the realloc shrink after
        val v = componentModelRealloc(0, 0, 50)
        val correct = componentModelRealloc(0, 0, 20)

        // now free these again, so we're back to the start state
        componentModelRealloc(correct, 20, 0)
        componentModelRealloc(v, 50, 0)

        // allocate and shrink
        val overallocatedAddress = componentModelRealloc(0, 0, 100)
        val shrunkenAddress = componentModelRealloc(overallocatedAddress, 100, 50)
        assertEquals(overallocatedAddress, shrunkenAddress)

        // allocate again: when we allocate now, we should get the same address as in "correct" before
        val actual = componentModelRealloc(0, 0, 20)
        assertEquals(correct, actual)

        // free stuff
        componentModelRealloc(actual, 20, 0)
        componentModelRealloc(shrunkenAddress, 50, 0)
    }

    @Test
    fun reallocGrowingInPlaceFreesRestTest() {
        // get a benchmark: allocate 1000 bytes normally, then allocate 20. Those 20 should start at the same address as if we allocate 50, then grow in place by 50, then allocate 20 again
        // NOTE: the allocation is this huge, to attempt to guarantee we get a consecutive one (probably at the end of everything)
        val v = componentModelRealloc(0, 0, 1000)
        val correct = componentModelRealloc(0, 0, 20)
        // free these again to get back to the starting state
        componentModelRealloc(correct, 20, 0)
        componentModelRealloc(v, 1000, 0)

        val underallocatedAddress = componentModelRealloc(0, 0, 500)
        val extendedAllocationAddress = componentModelRealloc(underallocatedAddress, 500, 1000)
        assertEquals(underallocatedAddress, extendedAllocationAddress)

        // allocate again, we should be at the same state as if we had allocated 1000 immediately
        val actual = componentModelRealloc(0, 0, 20)
        assertEquals(correct, actual)

        // free
        componentModelRealloc(extendedAllocationAddress, 1000, 0)
        componentModelRealloc(actual, 20, 0)
    }

    @Test
    fun reallocGrowingWithCopyFreesOldTest() {
        val ungrowableMemoryAddress = componentModelRealloc(0, 0, 10)
        // now we'll do a "filler" allocation in the middle, just to make it impossible to grow the first one
        val filler = componentModelRealloc(0, 0, 10)

        writeNBytes(ungrowableMemoryAddress, 10, 42.toByte())
        // now try to grow the ungrowable memory (which must result in a copy)
        val newAddress = componentModelRealloc(ungrowableMemoryAddress, 10, 20)
        // check the copy itself
        assertNotEquals(ungrowableMemoryAddress, newAddress)
        assertBytesEquals(newAddress, 10, 42.toByte())

        // NOTE: implementation detail: for a fresh allocation we should go through the free list from the start, and thus get the same address again as the original ungrowableMemoryAddress, as that must be freed by the growing allocation above
        val actual = componentModelRealloc(0, 0, 10)
        assertEquals(ungrowableMemoryAddress, actual)

        // free
        componentModelRealloc(newAddress, 20, 0)
        componentModelRealloc(actual, 10, 0)
        componentModelRealloc(filler, 10, 0)
    }
}
