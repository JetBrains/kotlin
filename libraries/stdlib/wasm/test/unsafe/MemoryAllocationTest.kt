package test.wasm.unsafe

import kotlin.wasm.unsafe.*
import kotlin.test.*

@OptIn(UnsafeWasmApi::class)
class MemoryAllocationTest {
    val pageSize = 65_536

    @Test
    fun testWasmMemorySizeGrow() {
        val s1 = wasmMemorySize()
        val grow_res = wasmMemoryGrow(10)
        val s2 = wasmMemorySize()
        assertNotEquals(grow_res, -1)
        assertEquals(grow_res, s1)
        assertEquals(s2 - s1, 10)
    }

    @Test
    fun testScopedAllocator() {
        val sizes = listOf<Int>(1, 1, 2, 3, 8, 10, 305, 12_747, 31_999)
        val allocations = mutableListOf<Pointer>()
        withScopedMemoryAllocator { a ->
            for (size in sizes) {
                allocations += a.allocate(size)
            }
        }

        val allocations2 = mutableListOf<Pointer>()
        withScopedMemoryAllocator { a ->
            for (size in sizes) {
                allocations2 += a.allocate(size)
            }
        }

        // Test that we run withScopedMemoryAllocator body
        assertEquals(allocations.size, sizes.size)

        // Memory should be reusing in different scopes
        // NOTE: This is current impl detail and can be changed
        assertEquals(allocations, allocations2)

        // Allocations are aligned
        assertTrue(allocations.all { it % 8 == 0 })

        // Allocations are different
        assertTrue(allocations.distinct().size == allocations.size)

        // Allocations do not intersect
        for (i1 in 0..<sizes.size) {
            for (i2 in (i1 + 1)..<sizes.size) {
                val a1 = allocations[i1]
                val a2 = allocations[i2]
                val size1 = sizes[i1]
                val size2 = sizes[i2]
                assertTrue(a1 !in a2..<a2 + size2)
                assertTrue(a1 + size1 - 1 !in a2..<a2 + size2)
            }
        }
    }

    @Test
    fun testScopedAllocatorGrowsMemory() {
        // Allocations past current memory size should grow memory
        val memSizes = mutableListOf<Pointer>(wasmMemorySize())
        withScopedMemoryAllocator { a ->
            var allocatedAddress = a.allocate(pageSize)
            var allocationSize = pageSize

            repeat(10) {
                var currPagesUsed = (allocatedAddress + allocationSize + 1) / pageSize
                var currPagesAvailable = wasmMemorySize()
                assertTrue(currPagesAvailable > currPagesUsed)
                // Allocate 10 pages past max page
                allocationSize = (currPagesAvailable - currPagesUsed + 10) * pageSize
                allocatedAddress = a.allocate(allocationSize)
                memSizes += wasmMemorySize()
            }
        }
        assertTrue(memSizes.size == 11)
        assertEquals(memSizes.distinct(), memSizes)
        assertEquals(memSizes.sorted(), memSizes)
    }

    @Test
    fun testScopedAllocatorThrows() {
        assertFailsWith<IllegalStateException> {
            withScopedMemoryAllocator {
                withScopedMemoryAllocator {
                }
            }
        }

        assertFailsWith<IllegalStateException> {
            var leakedAllocator: MemoryAllocator? = null
            withScopedMemoryAllocator { allocator ->
                leakedAllocator = allocator
            }
            leakedAllocator?.allocate(10)
        }

        assertFailsWith<IllegalStateException> {
            var leakedAllocator: MemoryAllocator? = null
            try {
                withScopedMemoryAllocator { allocator ->
                    leakedAllocator = allocator
                    throw Error()
                }
            } catch (e: Throwable) {
            }
            leakedAllocator?.allocate(10)
        }

        assertFailsWith<IllegalStateException> {
            fun foo(): MemoryAllocator {
                withScopedMemoryAllocator { allocator ->
                    return allocator  // non-local return
                }
            }
            foo().allocate(10)
        }
    }
}