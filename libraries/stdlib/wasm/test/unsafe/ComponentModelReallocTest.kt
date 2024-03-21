package test.wasm.unsafe

import kotlin.wasm.unsafe.*
import kotlin.test.*

@OptIn(UnsafeWasmMemoryApi::class)
class ReallocTest {
    @Test
    fun freshReallocTest() {
        val address1 = componentModelRealloc(0, 0, 10)
        val address2 = componentModelRealloc(0, 0, 10)
        freeAllComponentModelReallocAllocatedMemory()
        assertNotEquals(address1, address2)

        val address3 = componentModelRealloc(0, 0, 10)
        val address4 = componentModelRealloc(0, 0, 10)
        freeAllComponentModelReallocAllocatedMemory()

        // After freeing memory, new reallocs should reuse the old memory
        assertEquals(address1, address3)
        assertEquals(address4, address4)
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
            assertEquals(newAddress1, address1)
        }

        val address2 = componentModelRealloc(0, 0, 10)
        assertTrue(address2 - address1 >= allocationStepSize * numReallocs)

        freeAllComponentModelReallocAllocatedMemory()
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
        freeAllComponentModelReallocAllocatedMemory()

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

        freeAllComponentModelReallocAllocatedMemory()
    }

    @Test
    fun reallocInNestedScopeTest() {
        withScopedMemoryAllocator { allocator ->
            val scopedAddr = allocator.allocate(10)
            val reallocAddr = componentModelRealloc(0, 0, 10)
            freeAllComponentModelReallocAllocatedMemory()
            assertTrue(reallocAddr.toUInt() > scopedAddr.address)
            assertTrue((reallocAddr.toUInt() - scopedAddr.address) >= 10u)

            val scopedAddr2 = allocator.allocate(10)
            assertEquals(scopedAddr2.address.toInt(), reallocAddr)
        }
    }

    @Test
    fun creatingAllocatorsBeforeReallocIsFreedTest() {
        componentModelRealloc(0, 0, 10)
        assertFailsWith<IllegalStateException> {
            withScopedMemoryAllocator { allocator ->
                allocator.allocate(10)
            }
        }
        freeAllComponentModelReallocAllocatedMemory()
    }

}